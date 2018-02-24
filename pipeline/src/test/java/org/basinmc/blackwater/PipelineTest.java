package org.basinmc.blackwater;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskDependencyException;
import org.basinmc.blackwater.task.error.TaskException;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link Pipeline} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class PipelineTest {

  /**
   * Evaluates whether the pipeline correctly writes artifacts back into its cache.
   */
  @Test
  @SuppressWarnings("ConstantConditions")
  public void testArtifactCreation() throws TaskException, IOException {
    Path path = Paths.get("test.file");

    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    ArtifactManager manager = Mockito.mock(ArtifactManager.class);

    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Task 1");
    Mockito.when(task2.getName())
        .thenReturn("Task 1");

    Mockito.doAnswer(AdditionalAnswers.<Context>answerVoid((ctx) -> {
      Assert.assertNotNull(ctx.getInputPath());
      Assert.assertFalse(ctx.getInputPath().isPresent());

      Assert.assertNotNull(ctx.getOutputPath());
      Assert.assertTrue(ctx.getOutputPath().isPresent());
      Assert.assertNotEquals(path, ctx.getOutputPath().get());
    })).when(task1).execute(Mockito.any(Context.class));

    Mockito.doAnswer(AdditionalAnswers.<Context>answerVoid((ctx) -> {
      Assert.assertNotNull(ctx.getInputPath());
      Assert.assertFalse(ctx.getInputPath().isPresent());

      Assert.assertNotNull(ctx.getOutputPath());
      Assert.assertFalse(ctx.getOutputPath().isPresent());
    })).when(task2).execute(Mockito.any(Context.class));

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withArtifactManager(manager)
        .withTask(task1)
          .withOutputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    pipeline.execute();

    Mockito.verify(task1, Mockito.times(1))
        .execute(Mockito.notNull());
    Mockito.verify(task2, Mockito.times(1))
        .execute(Mockito.notNull());

    Mockito.verify(manager, Mockito.times(1))
        .createArtifact(Mockito.eq(reference), Mockito.argThat((p) -> !path.equals(p)));
  }

  /**
   * Evaluates whether the pipeline correctly fails when the artifact manager fails to create an
   * artifact.
   */
  @Test(expected = TaskExecutionException.class)
  public void testArtifactCreationException() throws TaskException, IOException {
    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    ArtifactManager manager = Mockito.mock(ArtifactManager.class);
    Mockito.when(manager.getArtifact(reference))
        .thenReturn(Optional.empty());
    Mockito.doAnswer(AdditionalAnswers.<ArtifactReference, Path>answerVoid((r, p) -> {
      Assert.assertEquals(reference, r);
      throw new IOException("Task Failure");
    })).when(manager).createArtifact(Mockito.eq(reference), Mockito.notNull());

    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Test 1");
    Mockito.when(task2.getName())
        .thenReturn("Test 2");

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withArtifactManager(manager)
        .withTask(task1)
          .withOutputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    try {
      pipeline.execute();
    } catch (TaskExecutionException ex) {
      Mockito.verify(task1, Mockito.times(1)).execute(Mockito.notNull());
      Mockito.verify(task2, Mockito.never()).execute(Mockito.any());

      throw ex;
    }
  }

  /**
   * Evaluates whether the pipeline correctly fails when no artifact manager is passed but an output
   * artifact is requested.
   */
  @Test(expected = TaskDependencyException.class)
  public void testArtifactCreationMissingManager() throws TaskException {
    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Task 1");
    Mockito.when(task2.getName())
        .thenReturn("Task 1");

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withTask(task1)
          .withOutputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    try {
      pipeline.execute();
    } catch (TaskDependencyException ex) {
      Mockito.verify(task1, Mockito.never())
          .execute(Mockito.any());
      Mockito.verify(task2, Mockito.never())
          .execute(Mockito.any());

      throw ex;
    }
  }

  /**
   * Evaluates whether the pipeline correctly skips execution of a task when its output artifact
   * already exists.
   */
  @Test
  public void testArtifactCreationSkip() throws TaskException, IOException {
    Path path = Paths.get("test.file");

    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    Artifact artifact = Mockito.mock(Artifact.class);
    Mockito.when(artifact.getPath())
        .thenReturn(path);

    ArtifactManager manager = Mockito.mock(ArtifactManager.class);
    Mockito.when(manager.getArtifact(reference))
        .thenReturn(Optional.of(artifact));

    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Test 1");
    Mockito.when(task2.getName())
        .thenReturn("Test 2");

    Mockito.when(task1.isValidArtifact(artifact, path))
        .thenReturn(true);
    Mockito.when(task2.isValidArtifact(artifact, path))
        .thenReturn(false);

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withArtifactManager(manager)
        .withTask(task1)
          .withOutputArtifact(reference)
          .register()
        .withTask(task2)
          .withOutputArtifact(reference)
          .register()
        .build();
    // @formatter:on

    pipeline.execute();

    Mockito.verify(task1, Mockito.times(1)).isValidArtifact(artifact, path);
    Mockito.verify(task2, Mockito.times(1)).isValidArtifact(artifact, path);

    Mockito.verify(task1, Mockito.never()).execute(Mockito.any());
    Mockito.verify(task2, Mockito.times(1)).execute(Mockito.notNull());

    Mockito.verify(manager, Mockito.times(2)).getArtifact(reference);
    Mockito.verify(manager, Mockito.times(1)).createArtifact(Mockito.eq(reference), Mockito.any());
    Mockito.verify(artifact, Mockito.times(2)).getPath();
  }

  /**
   * Evaluates whether the pipeline correctly retrieves an artifact from the artifact manager and
   * presents it to the dependant task.
   */
  @Test
  public void testArtifactRetrieval() throws TaskException, IOException {
    Path path = Paths.get("test.file");

    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    Artifact artifact = Mockito.mock(Artifact.class);
    Mockito.when(artifact.getPath())
        .thenReturn(path);

    ArtifactManager manager = Mockito.mock(ArtifactManager.class);
    Mockito.when(manager.getArtifact(reference))
        .thenReturn(Optional.of(artifact));

    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Test 1");
    Mockito.when(task2.getName())
        .thenReturn("Test 2");

    Mockito.doAnswer(AdditionalAnswers.<Context>answerVoid((ctx) -> {
      Assert.assertNotNull(ctx.getInputPath());
      Assert.assertTrue(ctx.getInputPath().isPresent());
      Assert.assertEquals(path, ctx.getInputPath().get());

      Assert.assertNotNull(ctx.getOutputPath());
      Assert.assertFalse(ctx.getOutputPath().isPresent());
    })).when(task1).execute(Mockito.any(Context.class));

    Mockito.doAnswer(AdditionalAnswers.<Context>answerVoid((ctx) -> {
      Assert.assertNotNull(ctx.getInputPath());
      Assert.assertFalse(ctx.getInputPath().isPresent());

      Assert.assertNotNull(ctx.getOutputPath());
      Assert.assertFalse(ctx.getOutputPath().isPresent());
    })).when(task2).execute(Mockito.any(Context.class));

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withArtifactManager(manager)
        .withTask(task1)
          .withInputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    pipeline.execute();

    Mockito.verify(task1, Mockito.times(1)).execute(Mockito.notNull());
    Mockito.verify(task2, Mockito.times(1)).execute(Mockito.notNull());

    Mockito.verify(manager, Mockito.times(1)).getArtifact(reference);
    Mockito.verify(artifact, Mockito.times(1)).getPath();
  }

  /**
   * Evaluates whether the pipeline correctly fails when the artifact manager cannot provide a
   * required artifact.
   */
  @Test(expected = TaskDependencyException.class)
  public void testArtifactRetrievalWithoutArtifact() throws TaskException, IOException {
    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Test 1");
    Mockito.when(task2.getName())
        .thenReturn("Test 2");

    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    ArtifactManager manager = Mockito.mock(ArtifactManager.class);
    Mockito.when(manager.getArtifact(reference))
        .thenReturn(Optional.empty());

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withArtifactManager(manager)
        .withTask(task1)
          .withInputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    try {
      pipeline.execute();
    } catch (TaskDependencyException ex) {
      Mockito.verify(task1, Mockito.never()).execute(Mockito.any());
      Mockito.verify(task2, Mockito.never()).execute(Mockito.any());

      Mockito.verify(manager, Mockito.times(1))
          .getArtifact(reference);
      Mockito.verifyNoMoreInteractions(manager);

      throw ex;
    }
  }

  /**
   * Evaluates whether the pipeline correctly fails its execution when an artifact is requested but
   * no manager is defined.
   */
  @Test(expected = TaskDependencyException.class)
  public void testArtifactRetrievalWithoutManager() throws TaskException {
    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Test 1");
    Mockito.when(task2.getName())
        .thenReturn("Test 2");

    ArtifactReference reference = Mockito.mock(ArtifactReference.class);
    Mockito.when(reference.getIdentifier())
        .thenReturn("test-artifact");

    // @formatter:off
    Pipeline pipeline = Pipeline.builder()
        .withTask(task1)
          .withInputArtifact(reference)
          .register()
        .withTask(task2)
          .register()
        .build();
    // @formatter:on

    try {
      pipeline.execute();
    } catch (TaskDependencyException ex) {
      Mockito.verify(task1, Mockito.never()).execute(Mockito.any());
      Mockito.verify(task2, Mockito.never()).execute(Mockito.any());

      throw ex;
    }
  }

  /**
   * Evaluates whether the pipeline executes all tasks within their respective order of
   * registration.
   */
  @Test
  public void testExecutionOrder() throws TaskException {
    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);
    Task task3 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Task 1");
    Mockito.when(task2.getName())
        .thenReturn("Task 2");
    Mockito.when(task3.getName())
        .thenReturn("Task 3");

    Pipeline pipeline = Pipeline.builder()
        .withTask(task1).register()
        .withTask(task2).register()
        .withTask(task3).register()
        .build();
    pipeline.execute();

    InOrder o = Mockito.inOrder(task1, task2, task3);

    o.verify(task1, Mockito.calls(1)).execute(Mockito.notNull());
    o.verify(task2, Mockito.calls(1)).execute(Mockito.notNull());
    o.verify(task3, Mockito.calls(1)).execute(Mockito.notNull());
  }

  /**
   * Evaluates whether the pipeline correctly fails when one of its tasks fails.
   */
  @Test(expected = TaskExecutionException.class)
  public void testExecutionException() throws TaskException {
    Task task1 = Mockito.mock(Task.class);
    Task task2 = Mockito.mock(Task.class);

    Mockito.when(task1.getName())
        .thenReturn("Task 1");
    Mockito.when(task2.getName())
        .thenReturn("Task 2");

    Mockito.doAnswer(AdditionalAnswers.<Context>answerVoid((ctx) -> {
      throw new TaskExecutionException("Test Failure");
    })).when(task1).execute(Mockito.notNull());

    Pipeline pipeline = Pipeline.builder()
        .withTask(task1).register()
        .withTask(task2).register()
        .build();

    try {
      pipeline.execute();
    } catch (TaskExecutionException ex) {
      Mockito.verify(task1, Mockito.times(1)).execute(Mockito.any());
      Mockito.verify(task2, Mockito.never()).execute(Mockito.any());

      throw ex;
    }
  }
}
