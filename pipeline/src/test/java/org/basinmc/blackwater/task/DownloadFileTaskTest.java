package org.basinmc.blackwater.task;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.io.DownloadFileTask;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link DownloadFileTask} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DownloadFileTaskTest extends AbstractTaskTest {

  /**
   * Evaluates whether example.org is actually available before attempting to download contents from
   * it (and thus failing if the machine or the servers are offline).
   */
  @BeforeClass
  public static void checkConnection() {
    try {
      InetAddress address = InetAddress.getByName("example.org");
      Assume.assumeTrue("example.org is unreachable", address.isReachable(1000));
    } catch (IOException ex) {
      Assume.assumeNoException("example.org is unreachable", ex);
    }
  }

  /**
   * Evaluates whether the task correctly downloads an HTML document via unsecured HTTP.
   */
  @Test
  public void testDownloadHttp() throws IOException, TaskExecutionException {
    this.testDownload("http://www.example.org");
  }

  /**
   * Evaluates whether the task correctly downloads an HTML document via HTTPS.
   */
  @Test
  public void testDownloadHttps() throws IOException, TaskExecutionException {
    this.testDownload("https://www.example.org");
  }

  /**
   * Evaluates whether the task correctly fails when the file is not found on the server.
   */
  @Test(expected = TaskExecutionException.class)
  public void testDownload404() throws IOException, TaskExecutionException {
    this.testDownload("https://www.example.org/illegal-file-name");
  }

  /**
   * Downloads an arbitrary file and evaluates whether it has been written to disk correctly.
   */
  private void testDownload(@Nonnull String url)
      throws IOException, TaskExecutionException {
    Path outputFile = this.getBase().resolve("output.html");

    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getRequiredOutputPath())
        .thenReturn(outputFile);

    Task task = new DownloadFileTask(url);
    task.execute(context);

    // we don't own the domain thus we simply assume that contents in the file are equal to a
    // successful download (the task is sufficiently simple to let this slide)
    Assert.assertTrue(Files.exists(outputFile));
    Assert.assertNotEquals(0, Files.size(outputFile));
  }
}
