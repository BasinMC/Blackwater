package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a mail archive of all changes between the current and a reference branch.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitFormatPatchTask extends AbstractExecutableTask {

  private static final Logger logger = LoggerFactory.getLogger(GitFormatPatchTask.class);

  private final String referenceBranch;

  public GitFormatPatchTask(@Nonnull String referenceBranch) {
    this.referenceBranch = referenceBranch;
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

    if (Files.notExists(inputPath.resolve(".git"))) {
      throw new TaskExecutionException("No repository at output path");
    }

    Path outputPath = context.getRequiredOutputPath();

    if (outputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Output path cannot be on a custom filesystem");
    }

    // check whether the repository is in an unclean state first to give users some basic feedback
    // on whether all their changes are included
    FileRepositoryBuilder builder = new FileRepositoryBuilder()
        .setWorkTree(inputPath.toFile());

    try (Repository repository = builder.build()) {
      try (Git git = new Git(repository)) {
        if (!git.status().call().isClean()) {
          logger.warn("Repository is not in a clean state");
          logger.warn("Unstaged changes will be omitted");
        }
      }
    } catch (GitAPIException | IOException ex) {
      throw new TaskExecutionException("Failed to access repository: " + ex.getMessage(), ex);
    }

    // next up, we'll have to invoke the git executable again (since jgit does not provide this
    // functionality)
    logger.info("Generating patches against reference branch \"{}\"", this.referenceBranch);
    Path path = inputPath.relativize(outputPath);

    if (this.execute(
        new ProcessBuilder("git", "format-patch", "-p", "--minimal", "-N", "-o", path.toString(),
            this.referenceBranch).directory(inputPath.toFile())) != 0) {
      throw new TaskExecutionException("Failed to generate patches: Git command returned an error");
    }

    logger.info("  Success");
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "git-format-patch";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresInputParameter() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresOutputParameter() {
    return true;
  }
}
