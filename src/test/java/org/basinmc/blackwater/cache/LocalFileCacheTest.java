package org.basinmc.blackwater.cache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.artifact.cache.Cache;
import org.basinmc.blackwater.artifact.cache.LocalFileCache;
import org.junit.Assert;
import org.junit.Test;

/**
 * Evaluates whether the {@link LocalFileCache} implementation is within its expected bounds.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class LocalFileCacheTest {

  private static final ArtifactReference REFERENCE_STANDARD = new ArtifactReference("test", "1.0.0",
      "bin");
  private static final ArtifactReference REFERENCE_CLASSIFIER = new ArtifactReference("test",
      "test", "1.0.0", "bin");

  /**
   * Evaluates whether the implementation properly writes and retrieves its artifacts.
   */
  @Test
  public void testWriteGet() throws IOException {
    Path directory = Files.createTempDirectory("blackwater_test_");
    Path testFile = Files.createTempFile("blackwater_", "_test.bin");
    Cache cache = new LocalFileCache(directory);

    try {
      // first thing, we'll check whether the cache claims to contain either of our test artifacts
      // (this shouldn't typically occur but we account for complete stupidity here)
      Assert.assertFalse(cache.getArtifact(REFERENCE_STANDARD).isPresent());
      Assert.assertFalse(cache.getArtifact(REFERENCE_CLASSIFIER).isPresent());

      // we'll generate a new test file which helps us evaluate the implementation first
      // the contents are well known and thus easy to evaluate against
      Files.write(testFile, "test".getBytes(StandardCharsets.UTF_8));

      // write both artifacts into the cache (this shouldn't throw any exceptions, if it does we'll
      // fail immediately) before evaluating whether the paths differ from each other (this also
      // shouldn't occur)
      Path standardPath = cache.writeArtifact(REFERENCE_STANDARD, testFile);
      Path classifierPath = cache.writeArtifact(REFERENCE_CLASSIFIER, testFile);

      Assert.assertNotEquals(testFile, standardPath);
      Assert.assertNotEquals(testFile, classifierPath);
      Assert.assertNotEquals(standardPath, classifierPath);

      // since the files exist, we'll also ensure that their contents have the correct contents
      Assert.assertEquals("test",
          new String(Files.readAllBytes(standardPath), StandardCharsets.UTF_8));
      Assert.assertEquals("test",
          new String(Files.readAllBytes(classifierPath), StandardCharsets.UTF_8));

      // evaluate whether we are given the correct artifacts when retrieving items from the cache
      Assert.assertEquals(
          standardPath,
          cache.getArtifact(REFERENCE_STANDARD)
              .orElseThrow(() -> new AssertionError(
                  "Cache did not contain artifact with identification " + REFERENCE_STANDARD))
      );
      Assert.assertEquals(
          classifierPath,
          cache.getArtifact(REFERENCE_CLASSIFIER)
              .orElseThrow(() -> new AssertionError(
                  "Cache did not contain artifact with identification " + REFERENCE_STANDARD))
      );
    } finally {
      // even if we fail, we'll need to clean up our mess in the temporary directory as some systems
      // do not regularly clear their temp directories
      try {
        Files.delete(testFile);
      } catch (IOException ex) {
        ex.printStackTrace();
      }

      Iterator<Path> it = Files.walk(directory)
          .sorted((a, b) -> Math.min(1, Math.max(-1, b.getNameCount() - a.getNameCount())))
          .iterator();

      while (it.hasNext()) {
        try {
          Files.delete(it.next());
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
  }
}
