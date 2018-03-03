package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes a new git repository at the specified target path.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitInitTask implements Task {

  private static final Logger logger = LoggerFactory.getLogger(GitInitTask.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path inputPath = context.getRequiredInputPath();

    if (inputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Input path cannot be on a custom filesystem");
    }

    if (Files.exists(inputPath.resolve(".git"))) {
      logger.info("Repository exists - Skipped execution");
      return;
    }

    try {
      logger.info("Creating a new empty git repository");
      Git.init()
          .setDirectory(inputPath.toFile())
          .call();
    } catch (GitAPIException ex) {
      throw new TaskExecutionException("Failed to create repository: " + ex.getMessage(), ex);
    }
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
  public boolean requiresInputParameter() {
    return true;
  }
}
