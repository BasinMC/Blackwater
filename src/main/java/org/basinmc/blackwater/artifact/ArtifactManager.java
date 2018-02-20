package org.basinmc.blackwater.artifact;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.basinmc.blackwater.artifact.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Abstracts cache access and handles artifacts throughout their lifetime.</p>
 *
 * <p>To reduce lookup times when using more complex caching implementations (and facilitate basic
 * support for cache-less environments), the manager will cache the paths to previously generated or
 * otherwise accessed artifacts).</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ArtifactManager {

  private static final Logger logger = LoggerFactory.getLogger(ArtifactManager.class);

  private final Cache cache;
  private final Map<ArtifactReference, Artifact> cachedPaths = new HashMap<>();

  public ArtifactManager(@Nullable Cache cache) {
    this.cache = cache;
  }

  /**
   * Retrieves an artifact which has previously been registered with the manager.
   *
   * @param reference an artifact reference.
   * @return a reference to the artifact or, if none has been registered yet, an empty optional.
   */
  @NonNull
  public Optional<Artifact> getArtifact(@NonNull ArtifactReference reference) {
    Artifact cachedArtifact = this.cachedPaths.get(reference);

    if (cachedArtifact != null) {
      return Optional.of(cachedArtifact);
    }

    return Optional.ofNullable(this.cache)
        .flatMap((c) -> c.getArtifact(reference))
        .map((p) -> {
          Artifact artifact = new Artifact(reference, p);
          this.cachedPaths.put(reference, artifact);
          return artifact;
        });
  }

  /**
   * Evaluates whether or not caching is currently enabled.
   *
   * @return true if caching, false otherwise.
   */
  public boolean isCachingEnabled() {
    return this.cache != null;
  }

  /**
   * Registers a new artifact with the manager.
   *
   * @param reference an artifact reference.
   * @param path a temporary path to the artifact.
   * @return a reference to the new artifact.
   */
  @NonNull
  public Artifact registerArtifact(@NonNull ArtifactReference reference, @NonNull Path path) {
    if (this.cache != null) {
      try {
        path = this.cache.writeArtifact(reference, path);
      } catch (IOException ex) {
        logger.warn("Failed to populate cache for artifact " + reference + ": " + ex.getMessage(),
            ex);
      }
    }

    Artifact artifact = new Artifact(reference, path);
    this.cachedPaths.put(reference, artifact);
    return artifact;
  }
}
