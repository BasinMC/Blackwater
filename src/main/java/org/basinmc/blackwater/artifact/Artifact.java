package org.basinmc.blackwater.artifact;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import javax.annotation.Nonnull;

/**
 * Represents a stored artifact which has been previously stored by a task and is now available for
 * reading.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public interface Artifact extends AutoCloseable {

  /**
   * {@inheritDoc}
   */
  @Override
  void close() throws IOException;

  /**
   * Retrieves the date and time at which this artifact was initially created.
   *
   * @return a timestamp.
   * @throws IOException when reading the creation timestamp fails.
   */
  @Nonnull
  Instant getCreationTimestamp() throws IOException;

  /**
   * Retrieves the date and time at which this artifact was last modified.
   *
   * @return a timestamp.
   * @throws IOException when reading the modification timestamp fails.
   */
  @Nonnull
  Instant getLastModificationTimestamp() throws IOException;

  /**
   * <p>Retrieves the path from which this artifact is available for reading.</p>
   *
   * <p>Since task output is stored as-is (e.g. files stay files and directories technically stay
   * directories), this path may be backed by a custom {@link java.nio.file.FileSystem} instance
   * which grants access to the archive contents.</p>
   *
   * @return a reference to the artifact contents.
   */
  @Nonnull
  Path getPath();

  /**
   * Retrieves the reference which uniquely identifies this artifact within its parent manager
   * implementation.
   *
   * @return a reference.
   */
  @Nonnull
  ArtifactReference getReference();
}
