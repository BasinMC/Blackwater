package org.basinmc.blackwater.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps an arbitrary resource to attach custom cleanup logic to its lifecycle.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CloseableResource<R, E extends Exception> implements AutoCloseable {

  private final R resource;
  private final CleanupProvider<E> cleanupProvider;

  public CloseableResource(@Nullable R resource, @Nonnull CleanupProvider<E> cleanupProvider) {
    this.resource = resource;
    this.cleanupProvider = cleanupProvider;
  }

  /**
   * Creates a closeable resource for a temporary directory.
   *
   * @return a resource.
   * @throws IOException when allocating a new temporary directory fails.
   */
  @Nonnull
  public static CloseableResource<Path, IOException> allocateTemporaryDirectory()
      throws IOException {
    Path tmp = Files.createTempDirectory("blackwater_");
    return new CloseableResource<>(tmp, () -> {
      Iterator<Path> it = Files.walk(tmp)
          .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
          .iterator();

      while (it.hasNext()) {
        Files.deleteIfExists(it.next());
      }
    });
  }

  /**
   * Creates a closeable resource for a temporary file.
   *
   * @return a resource.
   * @throws IOException when allocating a new temporary file fails.
   */
  @Nonnull
  public static CloseableResource<Path, IOException> allocateTemporaryFile() throws IOException {
    Path tmp = Files.createTempFile("blackwater_", ".tmp");
    return new CloseableResource<>(tmp, () -> Files.deleteIfExists(tmp));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws E {
    this.cleanupProvider.cleanup();
  }

  /**
   * Retrieves the actual wrapped resource.
   */
  public R getResource() {
    return this.resource;
  }

  /**
   * Provides the logic for a resource cleanup.
   *
   * @param <E> an exception type.
   */
  @FunctionalInterface
  public interface CleanupProvider<E extends Exception> {

    /**
     * Frees the resource.
     *
     * @throws E when the cleanup procedure fails.
     */
    void cleanup() throws E;
  }
}
