package org.basinmc.blackwater.task.io;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;

/**
 * Generates zip archives from all files within the source directory or file.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CreateArchiveTask implements Task {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path inputPath = context.getInputPath()
        .orElseThrow(() -> new TaskParameterException("Input artifact or path is required"));
    Path outputPath = context.getOutputPath()
        .orElseThrow(() -> new TaskParameterException("Output artifact or path is required"));

    Map<String, String> parameters = new HashMap<>();
    parameters.put("create", "true");

    try {
      URI uri = new URI("jar", outputPath.toAbsolutePath().toUri().toString(), null);

      try (FileSystem fs = FileSystems.newFileSystem(uri, parameters)) {
        Path base = fs.getPath("");

        if (Files.isRegularFile(inputPath)) {
          Files.copy(inputPath, base.resolve(inputPath.getFileName()));
          return;
        }

        Iterator<Path> it = Files.walk(inputPath)
            .filter((p) -> !inputPath.equals(p))
            .iterator();

        while (it.hasNext()) {
          Path source = it.next();
          Path target = base.resolve(inputPath.relativize(source).toString());

          if (Files.isDirectory(source)) {
            Files.createDirectories(target);
          } else {
            Files.copy(source, target);
          }
        }
      } catch (IOException ex) {
        throw new TaskExecutionException("Failed to create output archive: " + ex.getMessage(), ex);
      }
    } catch (URISyntaxException ex) {
      throw new TaskExecutionException("Failed to create output URI: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "create-archive";
  }
}
