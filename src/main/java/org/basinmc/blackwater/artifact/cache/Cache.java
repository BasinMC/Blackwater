package org.basinmc.blackwater.artifact.cache;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * Provides the necessary logic for caching artifacts (semi-)permanently within a nondescript
 * location.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public interface Cache {

  /**
   * Retrieves the path to a cached artifact or an empty optional, if no cache entry exists for the
   * requested artifact.
   *
   * @param reference an artifact reference.
   * @return a reference to the cached artifact file or an empty optional, if no cached version
   * exists.
   */
  @NonNull
  Optional<Path> getArtifact(@NonNull ArtifactReference reference);

  /**
   * <p>>Writes an artifact to the cache (overriding any existing artifact of the same
   * identification).</p>
   *
   * <p>Note that implementations, which do not rely on local storage, should at minimum copy
   * artifacts to a known location as the pipeline implementation may free the temporary file once
   * its task finishes.</p>
   *
   * @param reference an artifact reference.
   * @return a reference to the cached artifact file.
   * @throws IOException when writing to the cache fails.
   */
  Path writeArtifact(@NonNull ArtifactReference reference, @NonNull Path temporaryPath)
      throws IOException;
}
