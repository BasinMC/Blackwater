package org.basinmc.blackwater.task.error;

/**
 * Provides a base to exceptions which are thrown in cases where one or more tasks fail to execute
 * due to an expected problem within their execution (for instance, when accessing a required file
 * or contacting a server fails).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class TaskException extends Exception {

  public TaskException() {
  }

  public TaskException(String message) {
    super(message);
  }

  public TaskException(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskException(Throwable cause) {
    super(cause);
  }
}
