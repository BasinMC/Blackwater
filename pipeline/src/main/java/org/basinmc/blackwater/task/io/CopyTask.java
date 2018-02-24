package org.basinmc.blackwater.task.io;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;

/**
 * Copies a file or a directory of files to an arbitrary location.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CopyTask implements Task {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    try {
      Files.copy(
          context.getInputPath()
              .orElseThrow(() -> new TaskParameterException("Input artifact or path is required")),
          context.getOutputPath()
              .orElseThrow(() -> new TaskParameterException("Output artifact or path is required"))
      );
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to copy files: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "Copy";
  }
}
