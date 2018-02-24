package org.basinmc.blackwater.artifact;

import javax.annotation.Nonnull;

/**
 * <p>Provides a pointer to a specific artifact within an artifact manager.</p>
 *
 * <p>References always refer to a single specific artifact within the cache regardless of their
 * implementation. This means that requesting an artifact from the manager using the same reference
 * or a reference which is equal in value from the manager will always produce the same result.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface ArtifactReference {

  /**
   * Retrieves the human readable identifier which is represented by this reference (typically this
   * is some form of coordinate).
   *
   * @return a unique identifier for the referenced artifact.
   */
  @Nonnull
  String getIdentifier();
}
