package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Provides a task which commits staged changes to the repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitCommitTask implements Task {

  private final PersonIdent ident;
  private final String message;

  public GitCommitTask(@NonNull PersonIdent ident, @NonNull String message) {
    this.ident = ident;
    this.message = message;
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
        git.commit()
            .setCommitter(this.ident)
            .setMessage(this.message)
            .setAllowEmpty(false)
            .call();
      }
    } catch (GitAPIException ex) {
      throw new TaskExecutionException("Failed to execute git command: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to access repository: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "git-commit";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresInputParameter() {
    return true;
  }
}
