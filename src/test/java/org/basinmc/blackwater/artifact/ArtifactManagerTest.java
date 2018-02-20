package org.basinmc.blackwater.artifact;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.blackwater.artifact.cache.Cache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Provides test cases which evaluate whether {@link ArtifactManager} operates within its bounds.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ArtifactManagerTest {

  private static final ArtifactReference REFERENCE_CLASSIFIER = new ArtifactReference("test",
      "test", "1.0.0", "bin");
  private static final ArtifactReference REFERENCE_STANDARD = new ArtifactReference("test", "1.0.0",
      "bin");

  /**
   * Evaluates whether the manager's getter method correctly caches its artifacts.
   */
  @Test
  public void testGet() {
    Path testPath = Paths.get("test.bin");
    Cache cache = Mockito.mock(Cache.class);

    Mockito.when(cache.getArtifact(REFERENCE_STANDARD)).thenReturn(Optional.of(testPath));
    Mockito.when(cache.getArtifact(REFERENCE_CLASSIFIER)).thenReturn(Optional.empty());

    ArtifactManager manager = new ArtifactManager(cache);
    Assert.assertEquals(Optional.empty(), manager.getArtifact(REFERENCE_CLASSIFIER));
    Assert.assertEquals(Optional.empty(), manager.getArtifact(REFERENCE_CLASSIFIER));
    Assert.assertTrue(manager.getArtifact(REFERENCE_STANDARD).isPresent());

    Artifact artifact = manager.getArtifact(REFERENCE_STANDARD)
        .orElseThrow(() -> new AssertionError(
            "Cache did not contain artifact with identification " + REFERENCE_STANDARD));
    Assert.assertEquals(REFERENCE_STANDARD, artifact.getReference());
    Assert.assertEquals(testPath, artifact.getPath());

    Mockito.verify(cache, Mockito.times(1)).getArtifact(REFERENCE_STANDARD);
    Mockito.verify(cache, Mockito.times(2)).getArtifact(REFERENCE_CLASSIFIER);
  }

  /**
   * Evaluates whether the manager correctly registers new artifacts and writes them into the
   * cache.
   */
  @Test
  public void testRegister() throws IOException {
    Path testPath1 = Paths.get("test1.bin");
    Path testPath2 = Paths.get("test2.bin");
    Cache cache = Mockito.mock(Cache.class);

    Mockito.when(cache.writeArtifact(REFERENCE_STANDARD, testPath1)).thenReturn(testPath2);

    ArtifactManager manager = new ArtifactManager(cache);
    Artifact artifact = manager.registerArtifact(REFERENCE_STANDARD, testPath1);
    Assert.assertEquals(REFERENCE_STANDARD, artifact.getReference());
    Assert.assertEquals(testPath2, artifact.getPath());

    artifact = manager.getArtifact(REFERENCE_STANDARD)
        .orElseThrow(() -> new AssertionError(
            "Cache did not contain artifact with identification " + REFERENCE_STANDARD));
    Assert.assertEquals(REFERENCE_STANDARD, artifact.getReference());
    Assert.assertEquals(testPath2, artifact.getPath());

    Mockito.verify(cache, Mockito.times(1)).writeArtifact(REFERENCE_STANDARD, testPath1);
    Mockito.verify(cache, Mockito.never()).getArtifact(REFERENCE_STANDARD);
  }
}
