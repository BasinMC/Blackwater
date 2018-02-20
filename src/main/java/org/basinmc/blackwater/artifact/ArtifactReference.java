package org.basinmc.blackwater.artifact;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;

/**
 * Points to a unique artifact which may be written to or retrieved from an artifact cache or
 * generated as part of a pipeline task.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ArtifactReference {

  private final String classifier;
  private final String identifier;
  private final String type;
  private final String version;

  public ArtifactReference(
      @NonNull String identifier,
      @Nullable String classifier,
      @NonNull String version,
      @NonNull String type) {
    this.identifier = identifier;
    this.classifier = classifier;
    this.version = version;
    this.type = type;
  }

  public ArtifactReference(
      @NonNull String identifier,
      @NonNull String version,
      @NonNull String type) {
    this(identifier, null, version, type);
  }

  public ArtifactReference(
      @NonNull String identifier,
      @NonNull String version) {
    this(identifier, null, version, "jar");
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
    ArtifactReference that = (ArtifactReference) o;
    return Objects.equals(this.identifier, that.identifier) &&
        Objects.equals(this.classifier, that.classifier) &&
        Objects.equals(this.version, that.version) &&
        Objects.equals(this.type, that.type);
  }

  /**
   * @return an optional artifact classifier (or null when none is desired).
   */
  @Nullable
  public String getClassifier() {
    return this.classifier;
  }

  /**
   * @return a descriptive artifact identifier.
   */
  @NonNull
  public String getIdentifier() {
    return this.identifier;
  }

  /**
   * @return an artifact type identifier (typically a file extension).
   */
  @NonNull
  public String getType() {
    return this.type;
  }

  /**
   * @return an artifact revision.
   */
  @NonNull
  public String getVersion() {
    return this.version;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.identifier, this.classifier, this.version, this.type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "ArtifactReference{" +
        (
            "identifier=\"" + this.identifier + "\", " +
                "classifier=" + (this.classifier == null ? "null" : "\"" + this.classifier + "\"")
                + ", " +
                "version=\"" + this.version + "\", " +
                "type=\"" + this.type + "\""
        ) +
        "}";
  }
}
