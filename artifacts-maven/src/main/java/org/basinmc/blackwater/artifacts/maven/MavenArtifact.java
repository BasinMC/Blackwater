package org.basinmc.blackwater.artifacts.maven;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.artifact.Artifact;

/**
 * Represents an existing maven artifact.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MavenArtifact implements Artifact {

  private final FileSystem fileSystem;
  private final Path path;
  private final MavenArtifactReference reference;

  MavenArtifact(@Nonnull MavenArtifactReference reference, @Nonnull Path path)
      throws IOException {
    this.reference = reference;

    // we represent two distinctive types of artifacts here:
    //
    //   a) Regular files
    //      Which are stored as-is and interpreted as such by us (unless a user specifically
    //      requests directory access to a zip archive)
    //
    //   b) Directories
    //      Will be stored as zip archives as maven does not support storing of full archives within
    //      its repositories as is (logically)
    //
    if (!reference.isDirectory()) {
      // when we're dealing with case a, we'll interpret the artifact as-is
      this.path = path;
      this.fileSystem = null;
      return;
    }

    // in the second case, we're interpreting the artifact as a zip archive using NIO's filesystem
    try {
      this.fileSystem = FileSystems
          .newFileSystem(new URI("jar", path.toUri().toString(), null), Collections.emptyMap());
      this.path = this.fileSystem.getRootDirectories().iterator().next();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Illegal archive path: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    if (this.fileSystem == null) {
      return;
    }

    this.fileSystem.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MavenArtifact)) {
      return false;
    }
    MavenArtifact that = (MavenArtifact) o;
    return Objects.equals(this.reference, that.reference);
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
  public MavenArtifactReference getReference() {
    return this.reference;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.reference);
  }
}
