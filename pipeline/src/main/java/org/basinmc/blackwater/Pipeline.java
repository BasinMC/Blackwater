package org.basinmc.blackwater;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.ParameterBuilder;
import org.basinmc.blackwater.task.error.TaskDependencyException;
import org.basinmc.blackwater.task.error.TaskException;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.basinmc.blackwater.utility.CloseableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Provides a pre-configured pipeline of multiple tasks which are executed in the order they are
 * added.</p>
 *
 * <p>Tasks may generate artifacts and add them to an undefined caching system from where they may
 * be retrieved in order to speed up build times. Caching may be completely disabled during
 * construction of the pipeline.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class Pipeline {

  private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

  private final ArtifactManager artifactManager;
  private final List<TaskRegistration> taskQueue;

  private Pipeline(
      @Nullable ArtifactManager artifactManager,
      @NonNull List<TaskRegistration> tasks) {
    this.artifactManager = artifactManager;
    this.taskQueue = new ArrayList<>(tasks);
  }

  /**
   * Creates a new empty pipeline factory.
   *
   * @return a factory.
   */
  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Executes all tasks within the pipeline in their designated order (according to their respective
   * dependencies).
   *
   * @throws TaskDependencyException when task execution fails due to one or more missing
   * dependencies.
   * @throws TaskExecutionException when a task fails during its execution.
   * @throws TaskParameterException when one or more task parameters are outside of their expected
   * bounds.
   */
  public void execute() throws TaskException {
    for (TaskRegistration registration : this.taskQueue) {
      this.execute(registration);
    }
  }

  /**
   * Executes a single task registration.
   *
   * @param registration a registration.
   * @throws TaskException when the execution fails.
   */
  private void execute(@Nonnull TaskRegistration registration) throws TaskException {
    logger.info("--- Task {} ---", registration.task.getName());

    try (CloseableTaskResource input = this.getInputPath(registration);
        CloseableTaskResource output = this.getOutputPath(registration)) {
      // before we're just blindly executing the task, we'll evaluate whether its output artifact
      // already exists and is still considered valid to save ourselves some valuable time here
      if (!registration.enforceExecution && output.artifact != null) {
        assert registration.outputArtifact != null;
        logger.info("Evaluating cached version of artifact \"{}\"",
            registration.outputArtifact.getIdentifier());

        assert output.getResource() != null;
        if (registration.task.isValidArtifact(output.artifact, output.artifact.getPath())) {
          logger.info("Valid artifact cache - Skipped");
          return;
        }

        logger.info("Artifact expired - Recreating");
      } else if (registration.enforceExecution) {
        logger.info("Task execution enforced - Cache check omitted");
      }

      // since the cache does not contain a valid version of the task output (or no artifact is
      // being used), we have no choice but to execute the task
      try (CloseableResource<Map<String, Path>, IOException> parameterResource = this
          .populateParameterMap(registration)) {
        try (ContextImpl ctx = new ContextImpl(this.artifactManager, input.getResource(),
            output.getResource(), parameterResource.getResource())) {
          registration.task.execute(ctx);
        }
      } catch (IOException ex) {
        throw new TaskExecutionException(
            "Failed to close one or more artifact handles: " + ex.getMessage(), ex);
      }

      // if caching the task output in an artifact is desired, we'll have to write the task output
      // back to the artifact manager here
      if (registration.outputArtifact != null) {
        assert this.artifactManager != null;
        assert output.getResource() != null;

        try {
          this.artifactManager.createArtifact(registration.outputArtifact, output.getResource());
        } catch (IOException ex) {
          throw new TaskExecutionException(
              "Failed to store task output in artifact " + registration.outputArtifact
                  .getIdentifier() + ": " + ex.getMessage(), ex);
        }
      }
    }
  }

  /**
   * Retrieves a wrapped input path which is automatically cleaned up at the end of its lifecycle.
   *
   * @param registration a task registration.
   * @return a wrapped input path.
   * @throws TaskDependencyException when an input artifact is specified but no cached version
   * exists.
   */
  @Nonnull
  private CloseableTaskResource getInputPath(
      @Nonnull TaskRegistration registration) throws TaskDependencyException {
    // if we've been given a specific input file, we'll simply wrap the path and return it as-is as
    // we have no real reason to do any cleanup
    if (registration.inputFile != null) {
      return new CloseableTaskResource(registration.inputFile, null, () -> {
      });
    }

    // otherwise things are a little more complex as we'll have to find the referenced artifact and
    // wrap it for the purposes of this call
    if (registration.inputArtifact != null) {
      // since users can omit the artifact manager in cases where no task relies on it, we'll have
      // to ensure that there is one configured in this pipeline as well
      if (this.artifactManager == null) {
        throw new TaskDependencyException(
            "Unsatisfied task input: Cannot resolve artifact " + registration.inputArtifact
                .getIdentifier() + " without configured artifact manager");
      }

      try {
        Artifact artifact = this.artifactManager.getArtifact(registration.inputArtifact)
            .orElseThrow(() -> new TaskDependencyException(
                "Unsatisfied task input: Cannot find cached version of artifact "
                    + registration.inputArtifact.getIdentifier()));

        return new CloseableTaskResource(artifact.getPath(), artifact, () -> {
          try {
            artifact.close();
          } catch (IOException ex) {
            throw new TaskExecutionException(
                "Failed to release input artifact " + registration.inputArtifact.getIdentifier()
                    + ": " + ex.getMessage(), ex);
          }
        });
      } catch (IOException ex) {
        throw new TaskDependencyException(
            "Unsatisfied task input: Cannot access cached version of artifact "
                + registration.inputArtifact.getIdentifier() + ": " + ex.getMessage(), ex);
      }
    }

    // if no input has been specified at all, we'll simply wrap null
    return new CloseableTaskResource(null, null, () -> {
    });
  }

  /**
   * Retrieves a wrapped output path which is automatically cleaned up at the end of its lifecycle.
   *
   * @param registration a task registration.
   * @return a wrapped output path.
   * @throws TaskDependencyException when an output artifact is specified but no artifact manager is
   * configured.
   * @throws TaskExecutionException when creating the output path fails.
   */
  @Nonnull
  private CloseableTaskResource getOutputPath(
      @Nonnull TaskRegistration registration) throws TaskException {
    // if we've been given a specific output file, we'll simply wrap the path and return it as-is as
    // we have no real reason to do any cleanup
    if (registration.outputFile != null) {
      return new CloseableTaskResource(registration.outputFile, null, () -> {
      });
    }

    // otherwise things get a little more complicated as we'll have to allocate a temporary
    // directory to store the output file or directory in (we'll also need to clean these up again
    // obviously)
    if (registration.outputArtifact != null) {
      // since users can omit the artifact manager in cases where no task relies on it, we'll have
      // to ensure that there is one configured in this pipeline as well
      if (this.artifactManager == null) {
        throw new TaskDependencyException(
            "Unsatisfied task output: Cannot resolve artifact " + registration.outputArtifact
                .getIdentifier() + " without configured artifact manager");
      }

      try {
        Path basePath = Files.createTempDirectory("blackwater_task_");
        Path outputPath = basePath.resolve("output");

        Artifact artifact = this.artifactManager.getArtifact(registration.outputArtifact)
            .orElse(null);

        return new CloseableTaskResource(outputPath, artifact, () -> {
          try {
            Iterator<Path> it = Files.walk(basePath)
                .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
                .iterator();

            while (it.hasNext()) {
              Files.deleteIfExists(it.next());
            }
          } catch (IOException ex) {
            throw new TaskExecutionException(
                "Failed to clean up temporary task output: " + ex.getMessage(), ex);
          }
        });
      } catch (IOException ex) {
        throw new TaskExecutionException(
            "Failed to allocate temporary output directory: " + ex.getMessage(), ex);
      }
    }

    // if no output is desired, we'll simply wrap null
    return new CloseableTaskResource(null, null, () -> {
    });
  }

  /**
   * Populates a map of parameters based on a task registration.
   *
   * @param registration a registration.
   * @return a map of parameter paths.
   * @throws TaskDependencyException when an artifact fails to resolve.
   */
  @NonNull
  private CloseableResource<Map<String, Path>, IOException> populateParameterMap(
      @NonNull TaskRegistration registration) throws TaskDependencyException {
    Map<String, Path> parameters = new HashMap<>(registration.pathParameters);

    if (registration.artifactParameters.isEmpty()) {
      return new CloseableResource<>(parameters, () -> {
      });
    }

    if (this.artifactManager == null) {
      throw new TaskDependencyException(
          "Unsatisfied task output: Cannot resolve artifact parameters without configured artifact manager");
    }

    Set<Artifact> artifacts = new HashSet<>();

    for (Map.Entry<String, ArtifactReference> entry : registration.artifactParameters.entrySet()) {
      try {
        Artifact artifact = this.artifactManager.getArtifact(entry.getValue())
            .orElseThrow(() -> new TaskDependencyException(
                "Unsatisfied task parameter: Cannot resolve artifact " + entry.getValue()
                    .getIdentifier() + " for parameter \"" + entry.getKey() + "\""));

        artifacts.add(artifact);
        parameters.put(entry.getKey(), artifact.getPath());
      } catch (IOException ex) {
        throw new TaskDependencyException(
            "Unsatisfied task parameter: Failed to access artifact " + entry.getValue()
                .getIdentifier() + " for parameter \"" + entry.getKey() + "\": " + ex.getMessage(),
            ex);
      }
    }

    return new CloseableResource<>(parameters, () -> {
      for (Artifact artifact : artifacts) {
        artifact.close();
      }
    });
  }

  /**
   * Provides a factory for pipeline instances.
   */
  public static final class Builder {

    private ArtifactManager artifactManager;
    private final List<TaskRegistration> registrations = new ArrayList<>();

    private Builder() {
    }

    /**
     * Passes the local builder object to the supplied consumer implementation to permit
     * externalized configuration without requiring a break up of the builder configuration itself.
     *
     * @param consumer an arbitrary consumer.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder apply(@NonNull Consumer<Builder> consumer) {
      consumer.accept(this);
      return this;
    }

    /**
     * Constructs a new pipeline using the configuration within this builder.
     *
     * @return a pipeline.
     */
    @Nonnull
    public Pipeline build() {
      return new Pipeline(this.artifactManager, this.registrations);
    }

    /**
     * Selects an artifact manager to retrieve/store artifacts from/in to speed up the pipeline
     * execution.
     *
     * @param artifactManager a custom artifact manager.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withArtifactManager(@Nonnull ArtifactManager artifactManager) {
      this.artifactManager = artifactManager;
      return this;
    }

    /**
     * Appends a new task to the factory configuration.
     *
     * @param task a task.
     * @return a reference to the task parameter builder.
     */
    @Nonnull
    public ParameterBuilder withTask(@Nonnull Task task) {
      return new ParameterBuilderImpl(task);
    }

    /**
     * Provides a factory for task registrations.
     */
    private final class ParameterBuilderImpl implements ParameterBuilder {

      private final Task task;
      private boolean enforceExecution;

      private ArtifactReference inputArtifact;
      private ArtifactReference outputArtifact;

      private Path inputFile;
      private Path outputFile;

      private final Map<String, ArtifactReference> artifactParameters = new HashMap<>();
      private final Map<String, Path> pathParameters = new HashMap<>();

      private ParameterBuilderImpl(@Nonnull Task task) {
        this.task = task;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public Builder register() throws TaskParameterException {
        if (this.task.requiresInputParameter() && this.inputArtifact == null
            && this.inputFile == null) {
          throw new TaskParameterException(
              "Illegal task configuration: Input parameter is required");
        }

        if (this.task.requiresOutputParameter() && this.outputArtifact == null
            && this.outputFile == null) {
          throw new TaskParameterException(
              "Illegal task configuration: Output parameter is required");
        }

        Set<String> populatedParameters = new HashSet<>();
        populatedParameters.addAll(this.artifactParameters.keySet());
        populatedParameters.addAll(this.pathParameters.keySet());

        Set<String> missingParameters = this.task.getRequiredParameterNames().stream()
            .filter((n) -> !populatedParameters.contains(n))
            .collect(Collectors.toSet());

        if (!missingParameters.isEmpty()) {
          StringBuilder builder = new StringBuilder(
              "Illegal task configuration: One or more parameters are missing:");
          builder.append(System.lineSeparator());

          missingParameters
              .forEach((p) -> builder.append(" * ").append(p).append(System.lineSeparator()));

          throw new TaskParameterException(builder.toString());
        }

        Builder.this.registrations.add(new TaskRegistration(
            this.task,
            this.enforceExecution,
            this.inputArtifact,
            this.outputArtifact,
            this.inputFile,
            this.outputFile,
            this.artifactParameters,
            this.pathParameters
        ));

        return Builder.this;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public ParameterBuilder withInputArtifact(@Nonnull ArtifactReference artifact) {
        this.inputFile = null;
        this.inputArtifact = artifact;
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public ParameterBuilder withInputFile(@Nonnull Path file) {
        this.inputArtifact = null;
        this.inputFile = file;
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public ParameterBuilder withOutputArtifact(@Nonnull ArtifactReference artifact) {
        this.outputFile = null;
        this.outputArtifact = artifact;
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public ParameterBuilder withOutputFile(@Nonnull Path file) {
        this.outputArtifact = null;
        this.outputFile = file;
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @NonNull
      @Override
      public ParameterBuilder withParameter(@NonNull String name,
          @NonNull ArtifactReference artifact)
          throws TaskParameterException {
        if (!this.task.getAvailableParameterNames().contains(name)) {
          throw new TaskParameterException("No such parameter: " + name);
        }

        this.pathParameters.remove(name);
        this.artifactParameters.put(name, artifact);
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @NonNull
      @Override
      public ParameterBuilder withParameter(@NonNull String name, @NonNull Path file)
          throws TaskParameterException {
        if (!this.task.getAvailableParameterNames().contains(name)) {
          throw new TaskParameterException("No such parameter: " + name);
        }

        this.artifactParameters.remove(name);
        this.pathParameters.put(name, file);
        return this;
      }

      /**
       * {@inheritDoc}
       */
      @Nonnull
      @Override
      public ParameterBuilder withForcedExecution(boolean value) {
        this.enforceExecution = value;
        return this;
      }
    }
  }

  /**
   * Provides an extension to the closeable resource implementation to permit passing of artifacts.
   */
  private static final class CloseableTaskResource extends
      CloseableResource<Path, TaskExecutionException> {

    private final Artifact artifact;

    private CloseableTaskResource(
        @Nullable Path resource,
        @Nullable Artifact artifact,
        @Nonnull CleanupProvider<TaskExecutionException> cleanupProvider) {
      super(resource, cleanupProvider);
      this.artifact = artifact;
    }
  }

  /**
   * Provides contextual information to tasks and manages their respective temporary files.
   */
  private static final class ContextImpl implements AutoCloseable, Task.Context {

    private final ArtifactManager artifactManager;

    private final Path inputPath;
    private final Path outputPath;
    private final Map<String, Path> parameters;

    private final List<Path> temporaryDirectories = new ArrayList<>();
    private final List<Path> temporaryFiles = new ArrayList<>();

    private ContextImpl(
        @Nullable ArtifactManager artifactManager,
        @Nullable Path inputPath,
        @Nullable Path outputPath,
        @NonNull Map<String, Path> parameters) {
      this.artifactManager = artifactManager;

      this.inputPath = inputPath;
      this.outputPath = outputPath;
      this.parameters = parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      try {
        Iterator<Path> it = this.temporaryDirectories.iterator();

        while (it.hasNext()) {
          Path directory = it.next();

          Iterator<Path> fileIterator = Files.walk(directory)
              .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
              .iterator();

          while (fileIterator.hasNext()) {
            Files.deleteIfExists(fileIterator.next());
          }

          it.remove();
        }

        it = this.temporaryFiles.iterator();

        while (it.hasNext()) {
          Files.deleteIfExists(it.next());
          it.remove();
        }
      } catch (IOException ex) {
        StringBuilder builder = new StringBuilder("Failed to delete one or more temporary files: ");
        builder.append(ex.getMessage());

        if (!this.temporaryDirectories.isEmpty() || this.temporaryFiles.isEmpty()) {
          builder.append(System.lineSeparator());

          if (!this.temporaryDirectories.isEmpty()) {
            builder.append(System.lineSeparator());
            builder.append("Remaining temporary directories:");
            builder.append(System.lineSeparator());

            this.temporaryDirectories.forEach((p) -> {
              builder.append(" * ");
              builder.append(p.toAbsolutePath());
              builder.append(System.lineSeparator());
            });
          }

          if (!this.temporaryFiles.isEmpty()) {
            builder.append(System.lineSeparator());
            builder.append("Remaining temporary files:");
            builder.append(System.lineSeparator());

            this.temporaryFiles.forEach((p) -> {
              builder.append(" * ");
              builder.append(p.toAbsolutePath());
              builder.append(System.lineSeparator());
            });
          }
        }

        throw new IOException(builder.toString(), ex);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path allocateTemporaryDirectory() throws IOException {
      Path directory = Files.createTempDirectory("blackwater_task_");
      this.temporaryDirectories.add(directory);
      return directory;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path allocateTemporaryFile() throws IOException {
      Path file = Files.createTempFile("blackwater_task_", ".tmp");
      this.temporaryFiles.add(file);
      return file;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<ArtifactManager> getArtifactManager() {
      return Optional.ofNullable(this.artifactManager);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Optional<Path> getInputPath() {
      return Optional.ofNullable(this.inputPath);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Optional<Path> getOutputPath() {
      return Optional.ofNullable(this.outputPath);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<Path> getParameterPath(@NonNull String name) {
      return Optional.ofNullable(this.parameters.get(name));
    }
  }

  /**
   * Represents a registered task and its respective execution and context parameters.
   */
  private static final class TaskRegistration {

    private final Task task;
    private final boolean enforceExecution;

    private final ArtifactReference inputArtifact;
    private final ArtifactReference outputArtifact;

    private final Path inputFile;
    private final Path outputFile;

    private final Map<String, ArtifactReference> artifactParameters;
    private final Map<String, Path> pathParameters;

    private TaskRegistration(
        @Nonnull Task task,
        boolean enforceExecution,
        @Nullable ArtifactReference inputArtifact,
        @Nullable ArtifactReference outputArtifact,
        @Nullable Path inputFile,
        @Nullable Path outputFile,
        @NonNull Map<String, ArtifactReference> artifactParameters,
        @NonNull Map<String, Path> pathParameters) {
      this.task = task;
      this.enforceExecution = enforceExecution;
      this.inputArtifact = inputArtifact;
      this.outputArtifact = outputArtifact;
      this.inputFile = inputFile;
      this.outputFile = outputFile;
      this.artifactParameters = new HashMap<>(artifactParameters);
      this.pathParameters = new HashMap<>(pathParameters);
    }
  }
}
