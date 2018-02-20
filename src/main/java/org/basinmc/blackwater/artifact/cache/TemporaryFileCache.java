package org.basinmc.blackwater.artifact.cache;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import org.basinmc.blackwater.artifact.ArtifactReference;

/**
 * <p>Provides a temporary file cache implementation which will store all of its artifacts in a
 * temporary directory and delete everything once {@link #close()} is called.</p>
 *
 * <p>The actual location of the artifact cache depends on the operating system. For instance,
 * Windows will typically choose {@code &lt;user.home&gt;/Local/Temp} while *NIX operating systems
 * will typically choose {@code /tmp/}.</p>
 *
 * <p>Note that {@link #close()} should always be called at the end of the cache's lifetime as some
 * operating systems (specifically Windows and rarely some specific Linux distributions) will not
 * automatically delete temporary files.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class TemporaryFileCache extends LocalFileCache implements AutoCloseable {

  public TemporaryFileCache() throws IOException {
    super(Files.createTempDirectory("blackwater_"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    Iterator<Path> it = Files.walk(this.getPath())
        .sorted((a, b) -> Math.min(1, Math.max(-1, b.getNameCount() - a.getNameCount())))
        .iterator();

    while (it.hasNext()) {
      Path path = it.next();
      Files.delete(path);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<Path> getArtifact(@NonNull ArtifactReference reference) {
    // just in case we ever deal with a left over directory (even though this is extremely
    // unlikely), we'll return an empty optional to force re-generation
    return Optional.empty();
  }
}
