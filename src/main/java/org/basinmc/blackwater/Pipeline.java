package org.basinmc.blackwater;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.Task;
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
        logger.info("Evaluating cached version of artifact {}", registration.outputArtifact);

        assert output.getResource() != null;
        if (registration.task.isValidArtifact(output.artifact, output.getResource())) {
          logger.info("Valid artifact cache - Skipped");
          return;
        }

        logger.info("Artifact expired - Recreating");
      } else if (registration.enforceExecution) {
        logger.info("Task execution enforced - Cache check omitted");
      }

      // since the cache does not contain a valid version of the task output (or no artifact is
      // being used), we have no choice but to execute the task
      registration.task.execute(new ContextImpl(input.getResource(), output.getResource()));

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

        return new CloseableTaskResource(outputPath, null, () -> {
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

    private final Path inputPath;
    private final Path outputPath;

    private final List<Path> temporaryDirectories = new ArrayList<>();
    private final List<Path> temporaryFiles = new ArrayList<>();

    private ContextImpl(@Nullable Path inputPath, @Nullable Path outputPath) {
      this.inputPath = inputPath;
      this.outputPath = outputPath;
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

    private TaskRegistration(
        @Nonnull Task task,
        boolean enforceExecution,
        @Nullable ArtifactReference inputArtifact,
        @Nullable ArtifactReference outputArtifact,
        @Nullable Path inputFile,
        @Nullable Path outputFile) {
      this.task = task;
      this.enforceExecution = enforceExecution;
      this.inputArtifact = inputArtifact;
      this.outputArtifact = outputArtifact;
      this.inputFile = inputFile;
      this.outputFile = outputFile;
    }
  }
}
