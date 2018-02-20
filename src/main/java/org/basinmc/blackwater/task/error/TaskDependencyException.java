package org.basinmc.blackwater.task.error;

/**
 * Provides an exception which is thrown in cases where the pipeline fails to resolve one or more
 * dependencies.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class TaskDependencyException extends TaskException {

  public TaskDependencyException() {
  }

  public TaskDependencyException(String message) {
    super(message);
  }

  public TaskDependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskDependencyException(Throwable cause) {
    super(cause);
  }
}
