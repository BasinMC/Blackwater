package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates whether {@link GitInitTask} behaves as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitInitTaskTest extends AbstractGitTaskTest {

  private static final Logger logger = LoggerFactory.getLogger(GitInitTaskTest.class);

  /**
   * Evaluates whether the task correctly creates a new repository.
   */
  @Test
  public void testCreation() throws TaskExecutionException, IOException {
    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getInputPath())
        .thenReturn(Optional.of(this.getBase()));

    Task task = new GitInitTask();
    task.execute(context);

    Path repositoryPath = this.getBase().resolve(".git");
    Assert.assertTrue(Files.exists(repositoryPath));
    Assert.assertNotEquals(0, Files.list(repositoryPath).count());
  }

  /**
   * Evaluates whether the git init task is correctly skipped when a repository already exists (or
   * rather when ".git" already exists).
   */
  @Test
  public void testSkippedCreation() throws TaskExecutionException, IOException, GitAPIException {
    Path repositoryPath = this.getBase().resolve(".git");
    Files.createDirectories(repositoryPath);

    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getInputPath())
        .thenReturn(Optional.of(this.getBase()));

    Task task = new GitInitTask();
    task.execute(context);

    Assert.assertTrue(Files.exists(repositoryPath));
    Assert.assertEquals(0, Files.list(repositoryPath).count());
  }
}
