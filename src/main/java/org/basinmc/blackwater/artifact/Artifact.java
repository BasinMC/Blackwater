package org.basinmc.blackwater.artifact;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an artifact which has been retrieved from the cache or generated during this build.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class Artifact {

  private final ArtifactReference reference;
  private final Path path;

  Artifact(@NonNull ArtifactReference reference, @NonNull Path path) {
    this.reference = reference;
    this.path = path;
  }

  /**
   * @return the artifact identification.
   */
  @NonNull
  public ArtifactReference getReference() {
    return this.reference;
  }

  /**
   * @return the location at which this artifact is currently accessible.
   */
  @NonNull
  public Path getPath() {
    return this.path;
  }

  /**
   * <p>Opens a new input stream for this artifact.</p>
   *
   * <p>Note that this implementation will not attempt to close the constructed input stream. It's
   * the caller's responsibility to correctly free their allocated resources.</p>
   *
   * @return a new input stream for this artifact's contents.
   * @throws IOException when accessing the artifact fails.
   */
  @NonNull
  public InputStream openStream() throws IOException {
    return Files.newInputStream(this.path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    Artifact artifact = (Artifact) o;
    return Objects.equals(this.reference, artifact.reference) &&
        Objects.equals(this.path, artifact.path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.reference, this.path);
  }
}
