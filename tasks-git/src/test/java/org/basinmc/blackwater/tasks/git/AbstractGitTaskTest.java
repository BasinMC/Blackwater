package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                "Failed to delete temporary file " + p.toAbsolutePath() + ": " + ex.getMessage(),
                ex);
          }
        });
  }

  public Path getBase() {
    return this.base;
  }
}
