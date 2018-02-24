package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Provides test cases which evaluate whether {@link GitApplyMailArchiveTask} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitApplyMailArchiveTaskTest extends AbstractExecutableGitTaskTest {

  /**
   * Evaluates whether the task correctly applies a set of patches to a test file.
   */
  @Test
  public void testExecute() throws IOException, GitAPIException, TaskExecutionException {
    this.extract("/am-apply/test", Paths.get("test"));
    this.extract("/am-apply/0001-Test-Patch.patch", Paths.get("patches/0001-Test-Patch.patch"));

    InitCommand command = Git.init()
        .setDirectory(this.getBase().toFile());

    try (Git git = command.call()) {
      // re-create the expected repository state manually here to create a realistic test
      // environment
      git.add()
          .addFilepattern("test")
          .call();
      git.commit()
          .setAuthor("John Doe", "john@example.org")
          .setCommitter("John Doe", "john@example.org")
          .setMessage("Initial Commit")
          .call();

      // now we can simply mock the context and run the task to produce the end result and compare
      // it against our target values
      Context context = Mockito.mock(Context.class);
      Mockito.when(context.getInputPath())
          .thenReturn(Optional.of(this.getBase().resolve("patches")));
      Mockito.when(context.getOutputPath())
          .thenReturn(Optional.of(this.getBase()));

      GitApplyMailArchiveTask task = new GitApplyMailArchiveTask();
      task.execute(context);

      String expected = "This is a patched test file.\n";
      String actual = new String(Files.readAllBytes(this.getBase().resolve("test")),
          StandardCharsets.UTF_8);

      Assert.assertEquals(expected, actual);
    }
  }
}
