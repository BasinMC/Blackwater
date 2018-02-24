package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.Pipeline;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;

/**
 * Provides a task of sorts which is executed within a specific order within a pipeline in order to
 * generate or consume artifacts.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface Task {

  /**
   * Handles the execution of the task (e.g. generates artifacts, validates the system
   * configuration, etc).
   *
   * @throws TaskExecutionException when the task fails to complete.
   * @throws TaskParameterException when the task parameters (such as input and output files) are
   * outside of their valid bounds.
   */
  void execute(@NonNull Context context) throws TaskExecutionException;

  /**
   * <p>Retrieves a human readable name for this task.</p>
   *
   * <p>The name returned by this method will be used for logging and other debugging purposes
   * only.</p>
   */
  @NonNull
  default String getName() {
    return this.getClass().getSimpleName();
  }

  /**
   * <p>Evaluates whether the supplied cached artifact is considered valid and thus whether or not
   * to invoke this task even when a cached artifact is already present</p>
   *
   * <p>Typically this evaluates whether the artifact contents have expired since the last
   * invocation or whether they still mirror the expected state of a larger process intensive
   * operation.</p>
   *
   * @param artifact a reference to the cached artifact.
   * @param contents a reference to the path at which contained files can be iterated and accessed.
   * @return true if the artifact is still valid, false otherwise.
   */
  default boolean isValidArtifact(@Nonnull Artifact artifact, @Nonnull Path contents) {
    return true;
  }

  /**
   * Provides an execution context to tasks during their invocation.
   */
  interface Context {

    /**
     * <p>Allocates a new temporary directory for the duration of the task execution.</p>
     *
     * <p>These directories and their contents will be cleared automatically at the end of the task
     * execution and will not be written into any sort of cache.</p>
     *
     * @return a path to the newly created directory.
     * @throws IOException when allocation of a new temporary directory fails.
     */
    Path allocateTemporaryDirectory() throws IOException;

    /**
     * <p>Allocates a new temporary file for the duration of the task execution.</p>
     *
     * <p>These files will be cleared automatically at the end of the task execution and will not be
     * written into any sort of cache.</p>
     *
     * @return a path to the newly created file.
     * @throws IOException when allocation of a new temporary file fails.
     */
    @NonNull
    Path allocateTemporaryFile() throws IOException;

    /**
     * Retrieves the location at which the set of input files (if any) are located.
     */
    @Nonnull
    Optional<Path> getInputPath();

    /**
     * Retrieves the location at which the set of output files (if any) are located.
     */
    @Nonnull
    Optional<Path> getOutputPath();
  }

  /**
   * Provides a factory for contextual task parameters (such as input and output tasks) as well as
   * some other execution related parameters.
   */
  interface ParameterBuilder {

    /**
     * Assembles the parameters within this builder and adds the task to the pipeline at its
     * designated position.
     */
    @Nonnull
    Pipeline.Builder register();

    /**
     * Selects an artifact as an input directory for the task.
     *
     * @param artifact an artifact reference.
     * @return a reference to this builder.
     */
    @Nonnull
    ParameterBuilder withInputArtifact(@Nonnull ArtifactReference artifact);

    /**
     * Selects a file or directory as an input for the task.
     *
     * @param file an input file or directory.
     * @return a reference to this builder.
     */
    @Nonnull
    ParameterBuilder withInputFile(@Nonnull Path file);

    /**
     * <p>Selects an artifact as an output directory for the task.</p>
     *
     * <p>Unless {@link #withForcedExecution(boolean)} is specified, the task will evaluate whether
     * the specified output artifact exists and is valid. If so, the task will not be executed in
     * order to permit following tasks to rely on the cached version of the task output.</p>
     *
     * @param artifact an artifact reference.
     * @return a reference to this builder.
     */
    @Nonnull
    ParameterBuilder withOutputArtifact(@Nonnull ArtifactReference artifact);

    /**
     * Selects a directory as an output for the task.
     *
     * @param file an output directory (or path in a custom {@link java.nio.file.FileSystem}).
     * @return a reference to this builder.
     */
    @Nonnull
    ParameterBuilder withOutputFile(@Nonnull Path file);

    /**
     * @see #withForcedExecution(boolean)
     */
    @Nonnull
    default ParameterBuilder withForcedExecution() {
      return this.withForcedExecution(true);
    }

    /**
     * Selects whether or not the pipeline is permitted to rely on cached artifacts when they are
     * deemed valid or whether execution of the task is always expected.
     *
     * @param value if true executes the task at all times, otherwise evaluates cached artifacts.
     * @return a reference to this builder.
     */
    @Nonnull
    ParameterBuilder withForcedExecution(boolean value);
  }
}
