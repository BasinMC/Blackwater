package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
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
   * Retrieves the artifacts which this task is going to produce once invoked in its current
   * configuration.
   *
   * @return a set of artifact references.
   * @see #getRequiredArtifacts()
   */
  @NonNull
  default Set<ArtifactReference> getCreatedArtifacts() {
    return Collections.emptySet();
  }

  /**
   * <p>Retrieves a set of tasks which this task intends to run after (if present within the
   * pipeline).</p>
   *
   * <p>The pipeline will place a soft requirement on all indicated tasks (e.g. this task will be
   * executed after them but will not fail if these tasks are missing).</p>
   *
   * @see #getRequiredArtifacts()
   * @see #getRequiredTasks()
   */
  @NonNull
  default Set<Class<? extends Task>> getOptionalTasks() {
    return Collections.emptySet();
  }

  /**
   * <p>Retrieves a set of artifacts which this task requires to be present within a cache or
   * generated in its current configuration.</p>
   *
   * <p>When a requirement on an artifact is indicated, the pipeline will assume a dependency on any
   * task capable of generating said artifact. If the artifact is already within the cache, the
   * generator task may still be skipped however.</p>
   *
   * @return a set of artifact references.
   * @see #getRequiredTasks()
   * @see #getOptionalTasks()
   */
  @NonNull
  default Set<ArtifactReference> getRequiredArtifacts() {
    return Collections.emptySet();
  }

  /**
   * <p>Retrieves a set of tasks which this particular task depends on (e.g. these tasks will need
   * to be configured within the pipeline in order to successfully execute).</p>
   *
   * <p>The pipeline will place a hard requirement on all the indicated tasks (or any of their
   * respective specialized implementations). Tasks which simply wish to be executed after another
   * task but do not require said task to be part of the pipeline, should return them from {@link
   * #getOptionalTasks()} instead.</p>
   *
   * @see #getRequiredArtifacts()
   * @see #getOptionalTasks()
   */
  @NonNull
  default Set<Class<? extends Task>> getRequiredTasks() {
    return Collections.emptySet();
  }

  /**
   * Evaluates whether this task permits skipping when its artifacts are present within the selected
   * cache implementation.
   *
   * @return true if skipping is permitted, false otherwise.
   */
  default boolean permitsSkipping() {
    return true;
  }

  /**
   * Provides a base for task builder implementations which make use of the {@link InputFile} API.
   *
   * @param <T> a task type.
   */
  @FunctionalInterface
  interface Builder<T extends Task> {

    /**
     * Constructs a new task instance using the properties within this builder.
     *
     * @param manager a reference to the new artifact manager instance.
     * @return a task instance.
     */
    @NonNull
    T build(@NonNull ArtifactManager manager);
  }

  /**
   * <p>Provides a comparator implementation which sorts tasks in collections based on their
   * dependency definitions.</p>
   *
   * <p>Note that this implementation should never be used with implementations of {@link Set} as it
   * considers tasks which do not depend on each other in any way to be equal.</p>
   */
  class Comparator implements java.util.Comparator<Task> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(@NonNull Task task1, @NonNull Task task2) {
      // the highest priority we are dealing with is the task requirement as it provides us with the
      // strongest dependency expression
      if (this.containsClass(task2.getRequiredTasks(), task1.getClass())) {
        return -1;
      }

      if (this.containsClass(task1.getRequiredTasks(), task2.getClass())) {
        return 1;
      }

      // the second highest priority is the artifact dependency (as the task indicates that it
      // requires this artifact in order to function)
      if (this.intersect(task1.getCreatedArtifacts(), task2.getRequiredArtifacts())) {
        return -1;
      }

      if (this.intersect(task2.getCreatedArtifacts(), task1.getRequiredArtifacts())) {
        return 1;
      }

      // given that we've been through the requirements, we may now evaluate the second stage (e.g.
      // optional task requirements)
      if (this.containsClass(task2.getOptionalTasks(), task1.getClass())) {
        return -1;
      }

      if (this.containsClass(task1.getOptionalTasks(), task2.getClass())) {
        return 1;
      }

      // if we have no requirements, we'll consider the elements equal (e.g. their order does not
      // actually matter)
      // TODO: Possibly choose one task over the other to permit the use of sets here
      return 0;
    }

    /**
     * Evaluates whether a collection contains an exact match for the specified class or any of its
     * heirs.
     *
     * @param collection the collection to search.
     * @param type the type to search for.
     * @return true if an exact match or a heir is found, false otherwise.
     */
    private <T> boolean containsClass(
        @NonNull Collection<Class<? extends T>> collection,
        @NonNull Class<? extends T> type) {
      return collection.stream().anyMatch((c) -> c.isAssignableFrom(type));
    }

    /**
     * Evaluates whether two collections intersect (e.g. they have one or more elements in common).
     *
     * @return true when at least one common element is found, false otherwise.
     */
    private <T> boolean intersect(
        @NonNull Collection<? extends T> collection1,
        @NonNull Collection<? extends T> collection2) {
      return collection1.stream().anyMatch(collection2::contains);
    }
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
