package org.basinmc.blackwater.task;

import java.util.Collections;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.error.TaskDependencyException;
import org.basinmc.blackwater.task.error.TaskException;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Evaluates whether the {@link Pipeline} implementation behaves as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class PipelineTest {

  private static final ArtifactReference ARTIFACT_REFERENCE = new ArtifactReference(
      "test",
      "1.0.0",
      "bin"
  );

  /**
   * Evaluates whether the pipeline implementation correctly resolves the order of pipeline
   * elements.
   */
  @Test
  public void testExecution() throws TaskException {
    Task requiredTask = Mockito.mock(SecondaryTask.class);
    Task artifactProviderTask = Mockito.mock(Task.class);
    Task dependantTask = Mockito.mock(ThirdTask.class);
    Task optionallyDependantTask = Mockito.mock(Task.class);

    Mockito.when(artifactProviderTask.getCreatedArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));
    Mockito.when(artifactProviderTask.getRequiredTasks())
        .thenReturn(Collections.singleton(SecondaryTask.class));
    Mockito.when(dependantTask.getRequiredTasks())
        .thenReturn(Collections.singleton(SecondaryTask.class));
    Mockito.when(dependantTask.getRequiredArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));
    Mockito.when(optionallyDependantTask.getOptionalTasks())
        .thenReturn(Collections.singleton(ThirdTask.class));

    Pipeline pipeline = Pipeline.builder()
        .withTask(requiredTask)
        .withTask(dependantTask)
        .withTask(artifactProviderTask)
        .withTask(optionallyDependantTask)
        .build();

    pipeline.execute();

    InOrder order = Mockito.inOrder(
        requiredTask,
        artifactProviderTask,
        dependantTask,
        optionallyDependantTask
    );

    order.verify(requiredTask, Mockito.times(1)).execute(Mockito.any());
    order.verify(artifactProviderTask, Mockito.times(1)).execute(Mockito.any());
    order.verify(dependantTask, Mockito.times(1)).execute(Mockito.any());
    order.verify(optionallyDependantTask, Mockito.times(1)).execute(Mockito.any());
  }

  /**
   * Evaluates whether the pipeline implementation correctly identifies a valid configuration which
   * contains all possible dependency cases.
   */
  @Test
  public void testValidate() throws TaskDependencyException {
    Task requiredTask = Mockito.mock(SecondaryTask.class);
    Task artifactProviderTask = Mockito.mock(Task.class);
    Task dependantTask = Mockito.mock(Task.class);
    Task artifactDependantTask = Mockito.mock(Task.class);
    Task optionallyDependantTask = Mockito.mock(Task.class);

    Mockito.when(artifactProviderTask.getCreatedArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));
    Mockito.when(dependantTask.getRequiredTasks())
        .thenReturn(Collections.singleton(SecondaryTask.class));
    Mockito.when(artifactDependantTask.getRequiredArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));
    Mockito.when(optionallyDependantTask.getOptionalTasks())
        .thenReturn(Collections.singleton(SecondaryTask.class));

    Pipeline pipeline = Pipeline.builder()
        .withTask(artifactDependantTask)
        .withTask(requiredTask)
        .withTask(dependantTask)
        .withTask(artifactProviderTask)
        .build();

    pipeline.validate();
  }

  /**
   * Evaluates whether the pipeline implementation correctly identifies an invalid configuration in
   * which at least one explicitly required task is missing.
   */
  @Test(expected = TaskDependencyException.class)
  public void testValidateMissingDependency() throws TaskDependencyException {
    Task dependantTask = Mockito.mock(Task.class);
    Mockito.when(dependantTask.getRequiredTasks())
        .thenReturn(Collections.singleton(SecondaryTask.class));

    Pipeline pipeline = Pipeline.builder()
        .withTask(dependantTask)
        .build();

    pipeline.validate();
  }

  /**
   * Evaluates whether the pipeline implementation correctly identifies an invalid configuration in
   * which at least one explicitly required artifact is not created by any task.
   */
  @Test(expected = TaskDependencyException.class)
  public void testValidateMissingArtifact() throws TaskDependencyException {
    Task dependantTask = Mockito.mock(Task.class);
    Mockito.when(dependantTask.getRequiredArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));

    Pipeline pipeline = Pipeline.builder()
        .withTask(dependantTask)
        .build();

    pipeline.validate();
  }

  /**
   * Evaluates whether the pipeline implementation correctly identifies an invalid configuration in
   * which multiple tasks generate the same artifact.
   */
  @Test(expected = TaskDependencyException.class)
  public void testValidateDuplicateArtifact() throws TaskDependencyException {
    Task generatorTask1 = Mockito.mock(Task.class);
    Task generatorTask2 = Mockito.mock(Task.class);

    Mockito.when(generatorTask1.getCreatedArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));
    Mockito.when(generatorTask2.getCreatedArtifacts())
        .thenReturn(Collections.singleton(ARTIFACT_REFERENCE));

    Pipeline pipeline = Pipeline.builder()
        .withTask(generatorTask1)
        .withTask(generatorTask2)
        .build();

    pipeline.validate();
  }

  private interface SecondaryTask extends Task {

  }

  private interface ThirdTask extends Task {

  }
}
