package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link GitCommitTask} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitCommitTaskTest extends AbstractGitTaskTest {

  /**
   * Evaluates whether the task executes correctly.
   */
  @Test
  public void testExecute() throws TaskExecutionException, IOException, GitAPIException {
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

      Context context = Mockito.mock(Context.class);
      Mockito.when(context.getRequiredInputPath())
          .thenReturn(this.getBase());

      Task task = new GitCommitTask(new PersonIdent("John Doe", "john@example.org"),
          "Test Message");
      task.execute(context);

      Status status = git.status().call();
      Assert.assertTrue(status.isClean());
      Assert.assertFalse(status.hasUncommittedChanges());

      Iterator<RevCommit> commits = git.log().call().iterator();
      Assert.assertTrue(commits.hasNext());

      RevCommit commit = commits.next();
      Assert.assertEquals("John Doe", commit.getAuthorIdent().getName());
      Assert.assertEquals("john@example.org", commit.getAuthorIdent().getEmailAddress());
      Assert.assertEquals("Test Message", commit.getFullMessage());

      Assert.assertFalse(commits.hasNext());
    }
  }

  /**
   * Evaluates whether the task correctly fails when no repository exists at the specified
   * position.
   */
  @Test(expected = TaskExecutionException.class)
  public void testMissingRepository() throws TaskExecutionException {
    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getRequiredInputPath())
        .thenReturn(this.getBase());

    Task task = new GitCommitTask(new PersonIdent("John Doe", "john@example.org"),
        "Test Message");
    task.execute(context);
  }
}
