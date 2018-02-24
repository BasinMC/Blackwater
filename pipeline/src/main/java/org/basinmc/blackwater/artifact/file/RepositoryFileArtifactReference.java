package org.basinmc.blackwater.artifact.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides an artifact reference to an artifact on the local file system which follows the maven
 * repository structure.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class RepositoryFileArtifactReference extends FileArtifactReference {

  private final String artifactId;
  private final String classifier;
  private final String groupId;
  private final String type;
  private final String version;

  public RepositoryFileArtifactReference(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nullable String classifier,
      @Nonnull String type) {
    super(generatePath(groupId, artifactId, version, classifier, type));
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
    this.type = type;
  }

  /**
   * Generates a standard maven repository path based on the supplied parameters.
   *
   * @param groupId a group identifier.
   * @param artifactId an artifact identifier.
   * @param version an artifact version.
   * @param classifier an (optional) artifact classifier.
   * @param type an artifact type.
   * @return a relative path.
   */
  @Nonnull
  private static Path generatePath(
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nullable String classifier,
      @Nonnull String type) {
    Path path = Paths.get(groupId.replace('.', '/'));
    path = path.resolve(artifactId);
    path = path.resolve(version);

    StringBuilder name = new StringBuilder();

    name.append(artifactId);
    name.append('-');

    name.append(version);

    if (classifier != null) {
      name.append('-');
      name.append(classifier);
    }

    name.append('.');
    name.append(type);

    return path.resolve(name.toString());
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
    builder.append(':');
    builder.append(this.version);
    if (this.classifier != null) {
      builder.append(':');
      builder.append(this.classifier);
    }
    builder.append(':');
    builder.append(this.type);
    return builder.toString();
  }

  @Nonnull
  public String getType() {
    return this.type;
  }

  @Nonnull
  public String getVersion() {
    return this.version;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    // @formatter:off
    return "RepositoryFileArtifactReference{" +
        "groupId=\"" + this.groupId + "\", " +
        "artifactId=\"" + this.artifactId + "\", " +
        "version=\"" + this.version + "\", " +
        "classifier=" + (this.classifier == null ? "null" : "\"" + this.classifier + "\"") + ", " +
        "type=\"" + this.type + "\"" +
    "}";
    // @formatter:on
  }
}
