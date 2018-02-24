package org.basinmc.blackwater.artifact.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * <p>Provides an artifact manager which relies on local files.</p>
 *
 * <p>The actual directory format is defined based on the artifact reference type passed to the
 * implementation (e.g. custom layouts can be achieved by implementing {@link
 * FileArtifactReference}).</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FileArtifactManager implements ArtifactManager {

  private final Path base;

  public FileArtifactManager(@Nonnull Path base) {
    this.base = base;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createArtifact(@Nonnull ArtifactReference reference, @Nonnull Path source)
      throws IOException {
    if (!(reference instanceof FileArtifactReference)) {
      throw new FileNotFoundException(
          "Illegal artifact reference of type " + reference.getClass().getName()
              + " and identifier " + reference.getIdentifier());
    }

    Path artifactPath = this.base.resolve(((FileArtifactReference) reference).getPath());
    Files.createDirectories(artifactPath.getParent());
    Files.copy(source, artifactPath);
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Optional<Artifact> getArtifact(@Nonnull ArtifactReference reference) throws IOException {
    if (!(reference instanceof FileArtifactReference)) {
      throw new FileNotFoundException(
          "Illegal artifact reference of type " + reference.getClass().getName()
              + " and identifier " + reference.getIdentifier());
    }

    Path artifactPath = this.base.resolve(((FileArtifactReference) reference).getPath());

    if (Files.notExists(artifactPath)) {
      return Optional.empty();
    }

    return Optional.of(new FileArtifact(artifactPath, (FileArtifactReference) reference));
  }
}
