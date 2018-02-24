package org.basinmc.blackwater.task.error;

/**
 * Provides an exception for error cases where the validation of task parameters fails.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class TaskParameterException extends TaskExecutionException {

  public TaskParameterException() {
  }

  public TaskParameterException(String message) {
    super(message);
  }

  public TaskParameterException(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskParameterException(Throwable cause) {
    super(cause);
  }
}
