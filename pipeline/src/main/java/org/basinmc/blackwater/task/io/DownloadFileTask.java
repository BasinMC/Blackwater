package org.basinmc.blackwater.task.io;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;

/**
 * Downloads an arbitrary file from the specified URL.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DownloadFileTask implements Task {

  private final URL fileUrl;

  public DownloadFileTask(@Nonnull URL fileUrl) {
    this.fileUrl = fileUrl;
  }

  public DownloadFileTask(@Nonnull String fileUrl) throws MalformedURLException {
    this(new URL(fileUrl));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path out = context.getRequiredOutputPath();

    try (InputStream inputStream = this.fileUrl.openStream()) {
      try (ReadableByteChannel inputChannel = Channels.newChannel(inputStream)) {
        try (FileChannel outputChannel = FileChannel
            .open(out, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
          outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
      }
    } catch (IOException ex) {
      throw new TaskExecutionException(
          "Failed to download file from URL " + this.fileUrl.toExternalForm() + ": " + ex
              .getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresOutputParameter() {
    return true;
  }
}
