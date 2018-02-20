package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * Provides a configurable task which may be extended by users in order to add dependencies to other
 * tasks or artifacts.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractConfigurableTask implements Task {

  protected final Set<Class<? extends Task>> optionalTasks = new HashSet<>();
  protected final Set<ArtifactReference> requiredArtifacts = new HashSet<>();
  protected final Set<Class<? extends Task>> requiredTasks = new HashSet<>();

  public AbstractConfigurableTask() {
  }

  protected AbstractConfigurableTask(
      @NonNull Set<Class<? extends Task>> optionalTasks,
      @NonNull Set<ArtifactReference> requiredArtifacts,
      @NonNull Set<Class<? extends Task>> requiredTasks) {
    this.optionalTasks.addAll(optionalTasks);
    this.requiredArtifacts.addAll(requiredArtifacts);
    this.requiredTasks.addAll(requiredTasks);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Set<Class<? extends Task>> getOptionalTasks() {
    return Collections.unmodifiableSet(this.optionalTasks);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Set<ArtifactReference> getRequiredArtifacts() {
    return Collections.unmodifiableSet(this.requiredArtifacts);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Set<Class<? extends Task>> getRequiredTasks() {
    return Collections.unmodifiableSet(this.requiredTasks);
  }

  /**
   * Provides a builder for fully configurable tasks.
   *
   * @param <T> the task type.
   */
  public abstract static class Builder<T extends AbstractConfigurableTask>
      implements Task.Builder<T> {

    protected final Set<Class<? extends Task>> optionalTasks = new HashSet<>();
    protected final Set<ArtifactReference> requiredArtifacts = new HashSet<>();
    protected final Set<Class<? extends Task>> requiredTasks = new HashSet<>();

    /**
     * Selects an optional dependency for the new task.
     *
     * @param task a task to place a soft dependency on.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder<T> withOptionalTask(@NonNull Class<? extends Task> task) {
      this.optionalTasks.add(task);
      return this;
    }

    /**
     * Selects an artifact dependency for the new task.
     *
     * @param reference an artifact to depend upon.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder<T> withRequiredArtifact(@NonNull ArtifactReference reference) {
      this.requiredArtifacts.add(reference);
      return this;
    }

    /**
     * Selects a dependency for the new task.
     *
     * @param task a task to place a hard dependency on.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder<T> withRequiredTask(@NonNull Class<? extends Task> task) {
      this.requiredTasks.add(task);
      return this;
    }
  }
}
