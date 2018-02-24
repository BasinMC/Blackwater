package org.basinmc.blackwater.artifact.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides test cases which evaluate whether {@link FileArtifactManager} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FileArtifactManagerTest {

  private static final Logger logger = LoggerFactory.getLogger(FileArtifactManagerTest.class);
  private static final String TEST_VALUE = "!!!_1234test1234_!!!";

  private Path base;

  /**
   * Creates a new temporary directory which will act as a base to the file artifact manager for the
   * duration of each test.
   */
  @Before
  public void setupBaseDirectory() throws IOException {
    this.base = Files.createTempDirectory("blackwater_test_");
  }

  /**
   * Deletes all files created by the previous test.
   */
  @After
  public void destroyBaseDirectory() throws IOException {
    Files.walk(this.base)
        .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
        .forEach((p) -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException ex) {
            logger.error("Failed to delete temporary file " + p.toAbsolutePath() +
                ": " + ex.getMessage(), ex);
          }
        });
  }

  /**
   * Evaluates whether the manager correctly stores and retrieves artifacts from its local directory
   * when the flat structure is used.
   */
  @Test
  public void testFlatStoreRetrieve() throws IOException {
    ArtifactReference reference = new FlatFileArtifactReference("test");
    ArtifactManager manager = new FileArtifactManager(this.base);

    {
      Optional<Artifact> artifact = manager.getArtifact(reference);
      Assert.assertNotNull(artifact);
      Assert.assertFalse(artifact.isPresent());
    }

    Instant testTime = Instant.now().minus(Duration.ofSeconds(10));

    {
      Path testFile = Files.createTempFile("blackwater_test_", ".tmp");

      try {
        Files.write(testFile, TEST_VALUE.getBytes(StandardCharsets.UTF_8));
        manager.createArtifact(reference, testFile);
      } finally {
        Files.deleteIfExists(testFile);
      }
    }

    {
      Path expectedPath = this.base.resolve("test");
      Optional<Artifact> artifact = manager.getArtifact(reference);

      Assert.assertNotNull(artifact);
      Assert.assertTrue(artifact.isPresent());

      Artifact a = artifact.get();
      Assert.assertEquals(reference, a.getReference());
      Assert.assertEquals(expectedPath, a.getPath());

      Instant creationTime = a.getCreationTimestamp();
      Assert.assertTrue(testTime.equals(creationTime) || testTime.isBefore(creationTime));

      Instant modificationTime = a.getLastModificationTimestamp();
      Assert.assertTrue(testTime.equals(modificationTime) || testTime.isBefore(modificationTime));

      String contents = new String(Files.readAllBytes(a.getPath()), StandardCharsets.UTF_8);
      Assert.assertEquals(TEST_VALUE, contents);
    }
  }

  /**
   * Evaluates whether the manager correctly stores and retrieves artifacts from its local directory
   * when the repository structure is used.
   */
  @Test
  public void testRepositoryStoreRetrieve() throws IOException {
    ArtifactReference reference = new RepositoryFileArtifactReference("org.basinmc.blackwater",
        "test", "1.0-SNAPSHOT", null, "txt");
    ArtifactManager manager = new FileArtifactManager(this.base);

    {
      Optional<Artifact> artifact = manager.getArtifact(reference);
      Assert.assertNotNull(artifact);
      Assert.assertFalse(artifact.isPresent());
    }

    Instant testTime = Instant.now().minus(Duration.ofSeconds(10));

    {
      Path testFile = Files.createTempFile("blackwater_test_", ".tmp");

      try {
        Files.write(testFile, TEST_VALUE.getBytes(StandardCharsets.UTF_8));
        manager.createArtifact(reference, testFile);
      } finally {
        Files.deleteIfExists(testFile);
      }
    }

    {
      Path expectedPath = this.base
          .resolve("org/basinmc/blackwater/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.txt");
      Optional<Artifact> artifact = manager.getArtifact(reference);

      Assert.assertNotNull(artifact);
      Assert.assertTrue(artifact.isPresent());

      Artifact a = artifact.get();
      Assert.assertEquals(reference, a.getReference());
      Assert.assertEquals(expectedPath, a.getPath());

      Instant creationTime = a.getCreationTimestamp();
      Assert.assertTrue(testTime.equals(creationTime) || testTime.isBefore(creationTime));

      Instant modificationTime = a.getLastModificationTimestamp();
      Assert.assertTrue(testTime.equals(modificationTime) || testTime.isBefore(modificationTime));

      String contents = new String(Files.readAllBytes(a.getPath()), StandardCharsets.UTF_8);
      Assert.assertEquals(TEST_VALUE, contents);
    }
  }
}
