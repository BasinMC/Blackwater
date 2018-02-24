package org.basinmc.blackwater.artifact;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Provides a management and storage system for artifacts used as inputs and outputs of tasks.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public interface ArtifactManager {

  /**
   * <p>Writes the supplied source file or directory into the cache with the specified artifact
   * identification.</p>
   *
   * <p>Note that the source path may be either a file or directory depending on the task from which
   * it originated. The manager is expected to return the artifact in a similar fashion (e.g. files
   * will be stored as-is, directories will be wrapped in archive but made accessible again through
   * a {@link java.nio.file.FileSystem} implementation or a temporarily copy of the original
   * directory).</p>
   *
   * @param reference a reference to the desired artifact.
   * @param source a reference to the source file or directory.
   * @throws IOException when writing to the cache fails.
   */
  void createArtifact(@Nonnull ArtifactReference reference, @Nonnull Path source)
      throws IOException;

  /**
   * Retrieves a cached artifact from within the manager (for instance, to use it as an input within
   * a task execution).
   *
   * @param reference a reference to the desired artifact.
   * @return a reference to the artifact or, if no cached version of the artifact exists yet, an
   * empty optional.
   * @throws IOException when opening the artifact for reading fails.
   */
  @Nonnull
  Optional<Artifact> getArtifact(@Nonnull ArtifactReference reference) throws IOException;
}
