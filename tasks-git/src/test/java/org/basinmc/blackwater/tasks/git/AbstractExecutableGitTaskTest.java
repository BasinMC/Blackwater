package org.basinmc.blackwater.tasks.git;

import java.io.IOException;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * Provides a base to tests which rely on a git executable within the system PATH.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractExecutableGitTaskTest extends AbstractGitTaskTest {

  /**
   * Evaluates whether git is available before blindly executing any contained tests.
   */
  @BeforeClass
  public static void ensureGitAvailability() {
    try {
      Process process = new ProcessBuilder("git", "--version")
          .start();
      Assume.assumeTrue("Git looks incompatible", process.waitFor() == 0);
    } catch (InterruptedException | IOException ex) {
      Assume.assumeNoException("Git executable is unavailable", ex);
    }
  }
}
