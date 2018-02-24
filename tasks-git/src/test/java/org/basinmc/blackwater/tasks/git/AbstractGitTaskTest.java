package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * Provides an abstract git task test implementation which provides a temporary directory for the
 * duration of the test.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractGitTaskTest {

  private Path base;

  /**
   * Creates a temporary test directory before each test invocation.
   */
  @Before
  public void createTestDirectory() throws IOException {
    this.base = Files.createTempDirectory("blackwater_test_");
  }

  /**
   * Destroys the temporary test directory after each test invocation.
   */
  @After
  public void destroyTestDirectory() throws IOException {
    Files.walk(this.base)
        .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
        .forEach((p) -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException ex) {
            LoggerFactory.getLogger(this.getClass()).error(
                "Failed to delete temporary file " + p.toAbsolutePath() + ": " + ex.getMessage());
          }
        });
  }

  /**
   * Extracts a resource file from within the test Class-Path to the current test directory.
   *
   * @param resourcePath a resource path.
   * @param relativePath a relative path.
   * @throws IOException when extraction fails.
   */
  protected void extract(@Nonnull String resourcePath, @Nonnull Path relativePath)
      throws IOException {
    Path targetPath = this.getBase().resolve(relativePath);
    Files.createDirectories(targetPath.getParent());

    try (ReadableByteChannel inputChannel = Channels
        .newChannel(this.getClass().getResourceAsStream(resourcePath))) {
      try (FileChannel outputChannel = FileChannel
          .open(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.WRITE)) {
        outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
      }
    }
  }

  public Path getBase() {
    return this.base;
  }
}
