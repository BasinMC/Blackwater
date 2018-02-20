package org.basinmc.blackwater.task.error;

/**
 * Provides an exception for cases where the task itself fails to execute.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class TaskExecutionException extends TaskException {

  public TaskExecutionException() {
  }

  public TaskExecutionException(String message) {
    super(message);
  }

  public TaskExecutionException(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskExecutionException(Throwable cause) {
    super(cause);
  }
}
