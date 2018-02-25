package org.basinmc.blackwater.artifacts.maven;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * References a specific artifact within a maven repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MavenArtifactReference implements ArtifactReference {

  /**
   * Defines teh standard packaging value for maven.
   */
  public static final String DEFAULT_PACKAGING = "jar";
  private final String artifactId;
  private final String classifier;
  private final boolean directory;
  private final String groupId;
  private final String packaging;
  private final String version;

  public MavenArtifactReference(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nonnull String packaging,
      @Nullable String classifier,
      boolean directory) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.packaging = packaging;
    this.classifier = classifier;
    this.directory = directory;
  }

  public MavenArtifactReference(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nonnull String packaging,
      boolean directory) {
    this(groupId, artifactId, version, packaging, null, directory);
  }

  public MavenArtifactReference(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      boolean directory) {
    this(groupId, artifactId, version, DEFAULT_PACKAGING, null, directory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MavenArtifactReference)) {
      return false;
    }
    MavenArtifactReference that = (MavenArtifactReference) o;
    return Objects.equals(this.groupId, that.groupId) &&
        Objects.equals(this.artifactId, that.artifactId) &&
        Objects.equals(this.version, that.version) &&
        Objects.equals(this.packaging, that.packaging) &&
        Objects.equals(this.classifier, that.classifier);
  }

  @Nonnull
  public String getArtifactId() {
    return this.artifactId;
  }

  @Nullable
  public String getClassifier() {
    return this.classifier;
  }

  @Nonnull
  public String getGroupId() {
    return this.groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getIdentifier() {
    StringBuilder builder = new StringBuilder();
    builder.append(this.groupId);
    builder.append(':');
    builder.append(this.artifactId);

    if (!DEFAULT_PACKAGING.equalsIgnoreCase(this.packaging)) {
      builder.append(':');
      builder.append(this.packaging);
    }

    if (this.classifier != null) {
      builder.append(':');
      builder.append(this.classifier);
    }

    builder.append(':');
    builder.append(this.version);

    return builder.toString();
  }

  @Nonnull
  public String getPackaging() {
    return this.packaging;
  }

  @Nonnull
  public String getVersion() {
    return this.version;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects
        .hash(this.groupId, this.artifactId, this.version, this.packaging, this.classifier);
  }

  /**
   * Evaluates whether the artifact was originally submitted as a directory and thus is to be
   * interpreted as such within tasks which make use of this artifact.
   *
   * @return true if directory, false if file.
   */
  public boolean isDirectory() {
    return this.directory;
  }
}
