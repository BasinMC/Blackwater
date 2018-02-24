package org.basinmc.blackwater.artifact.file;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * Provides an artifact reference which identifies where an artifact is stored within a local
 * directory structure.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class FileArtifactReference implements ArtifactReference {

  private final Path path;

  protected FileArtifactReference(@Nonnull Path path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FileArtifactReference)) {
      return false;
    }
    FileArtifactReference that = (FileArtifactReference) o;
    return Objects.equals(this.path, that.path);
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getIdentifier() {
    return this.path.getFileName().toString();
  }

  /**
   * Retrieves the relative path at which this artifact is expected to be located within the
   * directory structure.
   *
   * @return a relative path.
   */
  @Nonnull
  public Path getPath() {
    return this.path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.path);
  }
}
