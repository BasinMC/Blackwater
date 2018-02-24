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
      @NonNull ArtifactManager artifactManager,
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
   */
  public void execute() throws TaskException {
    for (TaskRegistration registration : this.taskQueue) {
      logger.info("--- Task {} ---", registration.task.getName());

      // before we can even think about executing the task, we'll have to evaluate whether there are
      // any inputs for this task and if so whether we have access to them (e.g. when we are passed
      // an input artifact, we'll have to fail early if it does not exist yet)
      Artifact inputArtifact = null;
      Path inputPath = registration.inputFile;

      try {
        if (registration.inputArtifact != null) {
          try {
            inputArtifact = this.artifactManager.getArtifact(registration.inputArtifact)
                .orElseThrow(() -> new TaskDependencyException(
                    "Unsatisfied task input: Artifact " + registration.inputArtifact
                        + " does not exist"));
            inputPath = inputArtifact.getPath();
          } catch (IOException ex) {
            throw new TaskDependencyException(
                "Unsatisfied task input: Cannot access cached artifact "
                    + registration.inputArtifact
                    .getIdentifier() + ": " + ex.getMessage(), ex);
          }
        }

        // next up, we'll have to figure out whether our output is going to a file or artifact (if
        // we're writing to an artifact, we'll have to consider caching most likely)
        Path tempBase = null;
        Path outputPath = registration.outputFile;

        try {
          if (registration.outputArtifact != null) {
            try {
              if (!registration.enforceExecution) {
                Artifact cachedArtifact = this.artifactManager
                    .getArtifact(registration.outputArtifact)
                    .orElse(null);

                if (cachedArtifact != null) {
                  logger.info("Evaluating cached version of output artifact {}",
                      registration.outputArtifact.getIdentifier());

                  if (registration.task.isValidArtifact(cachedArtifact, cachedArtifact.getPath())) {
                    logger.info("Cached result - Skipped");
                    continue;
                  }

                  logger.info("Cached artifact has expired - Recreating");
                }
              } else {
                logger.info("Task execution enforced - Skipped cache check");
              }
            } catch (IOException ex) {
              throw new TaskDependencyException(
                  "Unsatisfied task output: Cannot access cached artifact "
                      + registration.outputArtifact.getIdentifier() + ": " + ex.getMessage(), ex);
            }

            try {
              // in this case we'll allocate a temporary directory that we'll store all of our stuff
              // in for now (the path we're passing to the task is nondescript and may either be a
              // file or directory at the end of its invocation, handling is up to the artifact
              // manager
              tempBase = Files.createTempDirectory("blackwater_");
              outputPath = tempBase.resolve("output");
            } catch (IOException ex) {
              throw new TaskExecutionException(
                  "Failed to allocate temporary output directory: " + ex.getMessage(), ex);
            }
          }

          // we've collected all necessary parameters, thus we can now actually perform the task
          // execution itself
          try (ContextImpl ctx = new ContextImpl(inputPath, outputPath)) {
            registration.task.execute(ctx);
          } catch (IOException ex) {
            throw new TaskExecutionException("Failed to clean up: " + ex.getMessage(), ex);
          }

          // since the task executed just fine, we'll have to evaluate whether the task stores its
          // results in an artifact and if so, notify the artifact manager
          if (registration.outputArtifact != null) {
            logger.info("Writing task output back to artifact {}",
                registration.outputArtifact.getIdentifier());

            try {
              this.artifactManager.createArtifact(registration.outputArtifact, outputPath);
            } catch (IOException ex) {
              throw new TaskExecutionException(
                  "Failed to write artifact " + registration.outputArtifact.getIdentifier()
                      + " to cache: " + ex.getMessage(), ex);
            }
          }

          logger.info("Success");
        } finally {
          if (tempBase != null) {
            try {
              Iterator<Path> it = Files.walk(tempBase)
                  .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
                  .iterator();

              while (it.hasNext()) {
                Files.deleteIfExists(it.next());
              }
            } catch (IOException ex) {
              logger.error("Failed to clean up temporary execution files: " + ex.getMessage(), ex);
            }
          }
        }
      } finally {
        if (inputArtifact != null) {
          try {
            inputArtifact.close();
          } catch (IOException ignore) {
          }
        }
      }
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
