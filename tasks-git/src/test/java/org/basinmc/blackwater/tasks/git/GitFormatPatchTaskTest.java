package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link GitFormatPatchTask} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitFormatPatchTaskTest extends AbstractExecutableGitTaskTest {

  /**
   * Evaluates whether the task executes as expected.
   */
  @Test
  public void testExecute() throws IOException, GitAPIException, TaskExecutionException {
    try (Git git = Git.init().setDirectory(this.getBase().toFile()).call()) {
      Files.write(this.getBase().resolve("test"),
          "This is a test file".getBytes(StandardCharsets.UTF_8));

      git.add()
          .addFilepattern("test")
          .call();
      git.commit()
          .setCommitter("John Doe", "john@example.org")
          .setMessage("Initial Commit")
          .call();
      git.branchCreate()
          .setName("upstream")
          .call();

      Files.write(this.getBase().resolve("test"),
          "This is a patched test file".getBytes(StandardCharsets.UTF_8));

      git.add()
          .addFilepattern("test")
          .call();
      git.commit()
          .setCommitter("John Doe", "john@example.org")
          .setMessage("Update Commit")
          .call();
    }

    Path patchDirectory = this.getBase().resolve("patches");

    Context ctx = Mockito.mock(Context.class);
    Mockito.when(ctx.getRequiredInputPath())
        .thenReturn(this.getBase());
    Mockito.when(ctx.getRequiredOutputPath())
        .thenReturn(patchDirectory);

    Task task = new GitFormatPatchTask("upstream");
    task.execute(ctx);

    Path patchPath = patchDirectory.resolve("0001-Update-Commit.patch");
    Assert.assertTrue(Files.exists(patchPath));
    Assert.assertTrue(Files.isRegularFile(patchPath));
  }
}
