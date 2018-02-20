package org.basinmc.blackwater.artifact.cache;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * <p>Provides a local file based cache implementation which stores any generated artifacts
 * permanently within an arbitrary defined directory on the executing machine.</p>
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>Custom extensions to this implementation may override {@link #getFileName(String, String,
 * String, String)} in order to customize the format of the local cache. Unless modified, the
 * implementation will create a flat structure (e.g. all cached artifacts will be placed within the
 * same directory).</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class LocalFileCache implements Cache {

  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("\\W");
  private final Path path;

  public LocalFileCache(@NonNull Path path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<Path> getArtifact(@NonNull ArtifactReference reference) {
    return Optional.of(this.getFileName(reference))
        .map(this.path::resolve)
        .filter(Files::exists);
  }

  /**
   * Retrieves the expected name of an artifact based on its reference.
   *
   * @param reference an artifact reference.
   * @return an artifact file name.
   */
  @NonNull
  protected Path getFileName(ArtifactReference reference) {
    // normalize all of our values first to prevent broken implementations from somehow permanently
    // breaking the cache (for the most part special characters are safe but we'll take no risks
    // here)
    String identifier = SPECIAL_CHAR_PATTERN.matcher(reference.getIdentifier()).replaceAll("_");
    String version = SPECIAL_CHAR_PATTERN.matcher(reference.getVersion()).replaceAll("_");
    String classifier = reference.getClassifier();

    if (classifier != null) {
      classifier = SPECIAL_CHAR_PATTERN.matcher(classifier).replaceAll("_");
    }

    String type = SPECIAL_CHAR_PATTERN.matcher(reference.getType()).replaceAll("_");

    // apart from these fixes, we can simply pass the values as-is
    return Paths.get(
        identifier + "-" + (classifier == null ? "" : "-" + classifier) + version + "." + type);
  }

  /**
   * Retrieves the path in which this cache will store all of its artifacts.
   *
   * @return a reference to the storage directory.
   */
  @NonNull
  public Path getPath() {
    return this.path;
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Path writeArtifact(
      @NonNull ArtifactReference reference,
      @NonNull Path temporaryPath) throws IOException {
    Path artifactPath = this.path.resolve(this.getFileName(reference));

    if (!Files.exists(artifactPath.getParent())) {
      Files.createDirectories(artifactPath.getParent());
    }

    Files.copy(temporaryPath, artifactPath);
    return artifactPath;
  }
}
