package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.utility.ProcessGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a base to tasks which rely on an external git executable.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
abstract class AbstractExecutableTask implements Task {

  /**
   * Executes an arbitrary process (typically git) and returns its respective exit value (or throws
   * an exception if the process exceeds its timeout).
   *
   * @param builder a process builder.
   * @return the return value.
   * @throws TaskExecutionException when execution of the task fails.
   */
  protected int execute(@Nonnull ProcessBuilder builder) throws TaskExecutionException {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    try {
      Process process = builder.start();

      ProcessGobbler gobbler = new ProcessGobbler(process, logger);
      gobbler.start();

      try {
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
          process.destroyForcibly();
          logger.error("Process has exceeded timeout of 5 minutes - Killed");
          return -1;
        }
      } catch (InterruptedException ex) {
        logger.error("Interrupted while awaiting process exit", ex);
      }

      return process.exitValue();
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to execute git: " + ex.getMessage(), ex);
    }
  }
}
