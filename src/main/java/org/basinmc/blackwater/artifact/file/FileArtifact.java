package org.basinmc.blackwater.artifact.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.artifact.Artifact;

/**
 * Represents an artifact within a local directory structure.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FileArtifact implements Artifact {

  private final Path path;
  private final FileArtifactReference reference;

  FileArtifact(@Nonnull Path path, @Nonnull FileArtifactReference reference) {
    this.path = path;
    this.reference = reference;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Instant getCreationTimestamp() throws IOException {
    try {
      BasicFileAttributes attributes = Files.readAttributes(this.path, BasicFileAttributes.class);
      return attributes.creationTime().toInstant();
    } catch (UnsupportedOperationException ex) {
      return Instant.EPOCH;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Instant getLastModificationTimestamp() throws IOException {
    return Files.getLastModifiedTime(this.path).toInstant();
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Path getPath() {
    return this.path;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public FileArtifactReference getReference() {
    return this.reference;
  }
}
