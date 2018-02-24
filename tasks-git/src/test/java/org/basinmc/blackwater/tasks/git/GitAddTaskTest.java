package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link GitAddTask} operates correctly.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitAddTaskTest extends AbstractGitTaskTest {

  /**
   * Evaluates whether the task correctly filters files and adds them to the repository.
   */
  @Test
  public void testAdd() throws GitAPIException, IOException, TaskExecutionException {
    InitCommand command = Git.init()
        .setDirectory(this.getBase().toFile());

    try (Git git = command.call()) {
      Files.write(this.getBase().resolve("fileA"),
          "!!!_test123_!!!".getBytes(StandardCharsets.UTF_8));
      Files.write(this.getBase().resolve("fileB"),
          "!!!_test123_!!!".getBytes(StandardCharsets.UTF_8));

      Context context = Mockito.mock(Context.class);
      Mockito.when(context.getInputPath())
          .thenReturn(Optional.of(this.getBase()));

      Task task = new GitAddTask((p) -> Paths.get("fileA").equals(p));
      task.execute(context);

      Status status = git.status().call();
      Assert.assertEquals(1, status.getAdded().size());
      Assert.assertEquals("fileA", status.getAdded().iterator().next());
    }
  }
}
