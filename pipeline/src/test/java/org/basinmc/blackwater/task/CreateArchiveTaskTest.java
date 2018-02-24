package org.basinmc.blackwater.task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.io.CreateArchiveTask;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Provides test cases which evaluate whether {@link CreateArchiveTask} performs as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CreateArchiveTaskTest extends AbstractTaskTest {

  /**
   * Evaluates whether the task executes as expected.
   */
  @Test
  public void testExecute() throws IOException, TaskExecutionException, URISyntaxException {
    Path inputDirectory = this.getBase().resolve("input");
    Path outputFile = this.getBase().resolve("output");

    Files.createDirectories(inputDirectory);

    Files.write(inputDirectory.resolve("test1"), "This is a test".getBytes(StandardCharsets.UTF_8));
    Files.write(inputDirectory.resolve("test2"), "This is a test".getBytes(StandardCharsets.UTF_8));
    Files.write(inputDirectory.resolve("test3"), "This is a test".getBytes(StandardCharsets.UTF_8));

    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getInputPath())
        .thenReturn(Optional.of(inputDirectory));
    Mockito.when(context.getOutputPath())
        .thenReturn(Optional.of(outputFile));

    Task task = new CreateArchiveTask();
    task.execute(context);

    try (FileSystem fs = FileSystems
        .newFileSystem(new URI("jar", outputFile.toUri().toString(), null),
            Collections.emptyMap())) {
      Iterator<Path> it = Files.walk(fs.getPath(""))
          .filter(Files::isRegularFile)
          .iterator();

      int fileCount = 0;
      while (it.hasNext()) {
        ++fileCount;

        Path current = it.next();
        String contents = new String(Files.readAllBytes(current), StandardCharsets.UTF_8);

        Assert.assertEquals("This is a test", contents);
      }

      Assert.assertEquals(3, fileCount);
    }
  }
}
