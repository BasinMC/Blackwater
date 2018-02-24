package org.basinmc.blackwater.utility;

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
   * {@inheritDoc}
   */
  @Override
  public void close() throws E {
    this.cleanupProvider.cleanup();
  }

  /**
   * Retrieves the actual wrapped resource.
   */
  @Nullable
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
