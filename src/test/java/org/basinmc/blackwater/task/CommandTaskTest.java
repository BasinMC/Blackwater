package org.basinmc.blackwater.task;

import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Provides test cases which evaluate whether {@link CommandTask} operates within its bounds.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CommandTaskTest {

  /**
   * <p>Evaluates whether the task executes correctly.</p>
   *
   * <p>Note that this task is really basic in its nature as it should run on most operating systems
   * we are building on.</p>
   */
  @Test
  public void testExecution() throws TaskExecutionException {
    Context context = Mockito.mock(Context.class);

    CommandTask task = CommandTask.builder()
        .withCommand("java")
        .withArgument("-version")
        .build();
    task.execute(context);
  }
}
