package org.basinmc.blackwater.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * Provides a base for tests which evaluate the correct function of tasks.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractTaskTest {

  private Path base;

  /**
   * Allocates a new temporary directory for a test.
   */
  @Before
  public void allocateTestDirectory() throws IOException {
    this.base = Files.createTempDirectory("blackwater_test_");
  }

  /**
   * Deletes the temporary directory allocated for the execution of a previous test.
   */
  @After
  public void destroyTestDirectory() throws IOException {
    Files.walk(this.base)
        .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
        .forEach((p) -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException ex) {
            LoggerFactory.getLogger(this.getClass())
                .error("Failed to delete temporary file {}: {}", p.toAbsolutePath(),
                    ex.getMessage());
          }
        });
  }

  public Path getBase() {
    return this.base;
  }
}
