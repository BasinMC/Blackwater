package org.basinmc.blackwater.task.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.AbstractConfigurableTask;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a task which ensures that a git repository exists at the target location.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitInitializeRepositoryTask extends AbstractConfigurableTask {

  private static final Logger logger = LoggerFactory.getLogger(GitInitializeRepositoryTask.class);

  private final Path path;

  public GitInitializeRepositoryTask(@NonNull Path path) {
    this.path = path;
  }

  private GitInitializeRepositoryTask(
      @NonNull Path path,
      @NonNull Set<Class<? extends Task>> optionalTasks,
      @NonNull Set<ArtifactReference> requiredArtifacts,
      @NonNull Set<Class<? extends Task>> requiredTasks) {
    super(optionalTasks, requiredArtifacts, requiredTasks);
    this.path = path;
  }

  /**
   * Constructs a new task factory.
   *
   * @return a factory.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "git-init";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    logger.info("Creating repository in {}", this.path);

    // before we even bother to initialize a new repository we'll check whether there's an existing
    // repository and if so abort (note that this check is pretty basic and does not evaluate
    // whether the repository is corrupted but rather whether a concept such as a git repository
    // exists at the moment)
    Path gitPath = this.path.resolve(".git");

    if (Files.exists(gitPath)) {
      logger.info("Repository already exists - Skipping Task");
      return;
    }

    try {
      // since some implementations may pass us a directory which does not actually exist yet, we may
      // need to create the directory before initializing the repository
      Files.createDirectories(this.path);

      // otherwise, we can simply initialize the repository using jgit (the git executable is not
      // necessary here)
      new InitCommand()
          .setDirectory(this.path.toFile())
          .call();
    } catch (GitAPIException | IOException ex) {
      throw new TaskExecutionException("Failed to initialize repository: " + ex.getMessage(), ex);
    }
  }

  /**
   * Provides a factory for the git initialization task.
   */
  public static final class Builder extends
      AbstractConfigurableTask.Builder<GitInitializeRepositoryTask> {

    private Path path;

    private Builder() {
    }

    /**
     * Constructs a new git initialization task.
     *
     * @return a new task instance.
     * @throws IllegalStateException when no path has been configured.
     */
    @NonNull
    public GitInitializeRepositoryTask build() {
      if (this.path == null) {
        throw new IllegalStateException("Illegal task configuration: Directory path is required");
      }

      return new GitInitializeRepositoryTask(
          this.path,
          this.optionalTasks,
          this.requiredArtifacts,
          this.requiredTasks
      );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public GitInitializeRepositoryTask build(@NonNull ArtifactManager manager) {
      return this.build();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withOptionalTask(@NonNull Class<? extends Task> task) {
      super.withOptionalTask(task);
      return this;
    }

    /**
     * Specifies a path to initialize the repository in.
     *
     * @param path a path pointing to a directory in which the repository should be initialized.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withPath(@NonNull Path path) {
      this.path = path;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withRequiredArtifact(@NonNull ArtifactReference reference) {
      super.withRequiredArtifact(reference);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withRequiredTask(@NonNull Class<? extends Task> task) {
      super.withRequiredTask(task);
      return this;
    }
  }
}
