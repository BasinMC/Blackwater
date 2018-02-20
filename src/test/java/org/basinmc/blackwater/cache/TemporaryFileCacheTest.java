package org.basinmc.blackwater.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.artifact.cache.TemporaryFileCache;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Evaluates whether the {@link TemporaryFileCache} implementation is within its bounds.</p>
 *
 * <p>Note that the temporary cache is only an extension to the standard file cache backend and is
 * thus mostly covered by {@link LocalFileCacheTest}.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class TemporaryFileCacheTest {

  private static final ArtifactReference REFERENCE = new ArtifactReference("test", "1.0.0",
      "bin");

  /**
   * Evaluates whether the cache correctly returns no cached values and deletes its temporary
   * directory when closed.
   */
  @Test
  public void testCleanup() throws IOException {
    Path testFile = Files.createTempFile("blackwater_test_", ".bin");
    Path cacheDirectory;

    try (TemporaryFileCache cache = new TemporaryFileCache()) {
      cacheDirectory = cache.getPath();
      Assert.assertTrue(Files.exists(cacheDirectory));
      Assert.assertFalse(cache.getArtifact(REFERENCE).isPresent());

      Path cachedFilePath = cache.writeArtifact(REFERENCE, testFile);
      Assert.assertTrue(Files.exists(cachedFilePath));
      Assert.assertFalse(cache.getArtifact(REFERENCE).isPresent());
    } finally {
      Files.delete(testFile);
    }

    Assert.assertNotNull(cacheDirectory);
    Assert.assertFalse(Files.exists(cacheDirectory));
  }
}
