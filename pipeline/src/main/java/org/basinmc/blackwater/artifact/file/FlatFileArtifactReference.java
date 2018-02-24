package org.basinmc.blackwater.artifact.file;

import java.io.File;
import java.nio.file.Paths;
import javax.annotation.Nonnull;

/**
 * Provides an artifact reference to an artifact on the local file system which relies on a single
 * flat directory.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FlatFileArtifactReference extends FileArtifactReference {

  private final String fileName;

  public FlatFileArtifactReference(@Nonnull String fileName) {
    super(Paths.get(fileName));

    if (fileName.contains("/") || fileName.contains(File.separator)) {
      throw new IllegalArgumentException(
          "Invalid file name: Cannot contain /" + (!"/".equals(File.separator) ? " or "
              + File.separator : ""));
    }

    this.fileName = fileName;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  public String getFileName() {
    return this.fileName;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getIdentifier() {
    return this.fileName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "FlatFileArtifactReference{fileName=\"" + this.fileName + "\"}";
  }
}
