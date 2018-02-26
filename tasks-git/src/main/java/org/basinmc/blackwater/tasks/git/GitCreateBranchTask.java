package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new branch of a specified name (unless it already exists).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitCreateBranchTask implements Task {

  private static final Logger logger = LoggerFactory.getLogger(GitCreateBranchTask.class);

  private final String name;

  public GitCreateBranchTask(@NonNull String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path inputPath = context.getRequiredInputPath();

    if (inputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Input path cannot be on a custom filesystem");
    }

    FileRepositoryBuilder builder = new FileRepositoryBuilder()
        .setWorkTree(inputPath.toFile());

    try (Repository repository = builder.build()) {
      try (Git git = new Git(repository)) {
        logger.info("Checking branch status ...");
        List<Ref> branches = git.branchList()
            .call();

        if (branches.stream().anyMatch((r) -> ("refs/heads/" + this.name).equals(r.getName()))) {
          logger.info(" Exists - Skipping execution");
          return;
        }
        logger.info("  Not found");

        logger.info("Creating branch \"" + this.name + "\" ...");
        git.branchCreate()
            .setName(this.name)
            .call();
        logger.info("  Success");
      }
    } catch (GitAPIException ex) {
      logger.error("  Failed");
      throw new TaskExecutionException("Failed to create branch: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to access repository: " + ex.getMessage());
    }
  }
}
