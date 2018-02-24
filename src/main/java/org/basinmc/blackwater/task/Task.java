package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.task.error.TaskExecutionException;

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
   * @return true if the artifact is still valid, false otherwise.
   */
  default boolean isValidArtifact(@Nonnull Artifact artifact) {
    return true;
  }

  /**
   * Provides an execution context to tasks during their invocation.
   */
  interface Context {

    /**
     * <p>Allocates a new temporary file for the duration of the task execution.</p>
     *
     * <p>Files allocated through this method will be automatically deleted upon finalization of the
     * task execution. As such, they are suited as temporary storage for artifacts which are to be
     * written back into the cache.</p>
     *
     * @throws IOException when allocation of a new temporary file fails.
     */
    @NonNull
    Path allocateTemporaryFile() throws IOException;

    /**
     * Retrieves the artifact manager which is handling the permanent storage and caching of
     * artifacts.
     */
    @NonNull
    ArtifactManager getArtifactManager();
  }
}
