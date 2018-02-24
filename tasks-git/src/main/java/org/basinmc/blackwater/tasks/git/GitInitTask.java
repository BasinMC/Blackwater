package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Initializes a new git repository at the specified target path.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitInitTask implements Task {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path outputPath = context.getOutputPath()
        .orElseThrow(() -> new TaskParameterException("Output path is required"));

    if (outputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Output path cannot be on a custom filesystem");
    }

    try {
      new InitCommand()
          .setDirectory(outputPath.toFile())
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
}
