package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class GitInitializeRepositoryTask implements Task {

  private static final Logger logger = LoggerFactory.getLogger(GitInitializeRepositoryTask.class);

  private final Path path;

  public GitInitializeRepositoryTask(@NonNull Path path) {
    this.path = path;
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
}
