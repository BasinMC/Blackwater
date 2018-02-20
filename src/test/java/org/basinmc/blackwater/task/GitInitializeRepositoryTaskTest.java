package org.basinmc.blackwater.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link GitInitializeRepositoryTask} performs as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitInitializeRepositoryTaskTest {

  /**
   * Evaluates whether the task executes correctly.
   */
  @Test
  public void testExecution() throws IOException, TaskExecutionException {
    Context context = Mockito.mock(Context.class);
    Path path = Files.createTempDirectory("blackwater_test_");

    try {
      Task task = new GitInitializeRepositoryTask(path);
      task.execute(context);
    } finally {
      Files.walk(path)
          .sorted((a, b) -> Math.min(1, Math.max(-1, b.getNameCount() - a.getNameCount())))
          .forEach((p) -> {
            try {
              Files.delete(p);
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          });
    }
  }
}
