package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link GitCreateBranchTask} operates correctly.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitCreateBranchTaskTest extends AbstractGitTaskTest {

  /**
   * Evaluates whether the task correctly creates a new branch.
   */
  @Test
  public void testExecute() throws GitAPIException, TaskExecutionException, IOException {
    InitCommand command = Git.init()
        .setDirectory(this.getBase().toFile());

    try (Git git = command.call()) {
      Files.write(this.getBase().resolve("fileA"),
          "!!!_test123_!!!".getBytes(StandardCharsets.UTF_8));
      Files.write(this.getBase().resolve("fileB"),
          "!!!_test123_!!!".getBytes(StandardCharsets.UTF_8));

      git.add()
          .addFilepattern("fileA")
          .addFilepattern("fileB")
          .call();

      git.commit()
          .setCommitter("John Doe", "john@example.org")
          .setMessage("Test Commit")
          .call();

      Context context = Mockito.mock(Context.class);
      Mockito.when(context.getRequiredInputPath())
          .thenReturn(this.getBase());

      Task task = new GitCreateBranchTask("test");
      task.execute(context);

      List<Ref> branches = git.branchList()
          .call();

      Assert.assertEquals(2, branches.size());
      Assert.assertTrue(branches.stream().anyMatch((r) -> "refs/heads/master".equals(r.getName())));
      Assert.assertTrue(branches.stream().anyMatch((r) -> "refs/heads/test".equals(r.getName())));
    }
  }
}
