package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.artifact.cache.Cache;
import org.basinmc.blackwater.artifact.cache.LocalFileCache;
import org.basinmc.blackwater.artifact.cache.TemporaryFileCache;
import org.basinmc.blackwater.task.Task.Context;
import org.basinmc.blackwater.task.error.TaskDependencyException;
import org.basinmc.blackwater.task.error.TaskException;
import org.basinmc.blackwater.task.error.TaskExecutionException;

/**
 * <p>Provides a pre-configured pipeline of multiple tasks which are executed in the order they are
 * added.</p>
 *
 * <p>Tasks may generate artifacts and add them to an undefined caching system from where they may
 * be retrieved in order to speed up build times. Caching may be completely disabled during
 * construction of the pipeline.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class Pipeline {

  private final ArtifactManager artifactManager;

  private final ContextImpl context = new ContextImpl();
  private final List<Task> taskQueue = new ArrayList<>();

  private Pipeline(@NonNull ArtifactManager artifactManager, @NonNull Set<Task> tasks) {
    this.artifactManager = artifactManager;

    this.taskQueue.addAll(tasks);
    this.taskQueue.sort(new Task.Comparator());
  }

  /**
   * Constructs a new empty builder instance.
   *
   * @return an empty builder.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Executes all tasks within the pipeline in their designated order (according to their respective
   * dependencies).
   *
   * @throws TaskDependencyException when task execution fails due to one or more missing
   * dependencies.
   * @throws TaskExecutionException when a task fails during its execution.
   */
  public void execute() throws TaskException {
    this.validate();

    for (Task task : this.taskQueue) {
      task.execute(this.context);

      // if caching is enabled, we may clear our temporary files every time a task has finished its
      // execution as we won't accidentally delete any of the newly registered artifacts in this
      // case
      if (this.artifactManager.isCachingEnabled()) {
        try {
          this.context.releaseTemporaryFiles();
        } catch (IOException ex) {
          throw new TaskExecutionException("Failed to release temporary files: " + ex.getMessage(),
              ex);
        }
      }
    }

    try {
      this.context.releaseTemporaryFiles();
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to release temporary files: " + ex.getMessage(), ex);
    }
  }

  /**
   * Evaluates whether the pipeline contains all required tasks and artifacts.
   *
   * @throws TaskDependencyException when one or more tasks or artifacts are missing within the
   * pipeline.
   */
  public void validate() throws TaskDependencyException {
    // first of all, we'll evaluate whether all required tasks are present within the pipeline at
    // the moment as this is the most important constraint placed on any given task
    Set<Class<?>> missingTasks = this.taskQueue.stream()
        .flatMap((t) -> t.getRequiredTasks().stream())
        .filter((c) -> this.taskQueue.stream().noneMatch(c::isInstance))
        .collect(Collectors.toSet());

    if (!missingTasks.isEmpty()) {
      StringBuilder message = new StringBuilder("One or more unsatisfied dependencies:");
      message.append(System.lineSeparator());

      missingTasks.forEach((t) -> {
        message.append("    * ");
        message.append(t.getName());
        message.append(System.lineSeparator());
      });

      throw new TaskDependencyException(message.toString());
    }

    // next up, we'll make sure that all artifact references are satisfied (and whether there is any
    // collisions)
    Set<ArtifactReference> generatedArtifacts = new HashSet<>();
    Map<ArtifactReference, Task> taskRegistration = new HashMap<>();
    Map<ArtifactReference, Set<Task>> duplicatedArtifacts = new HashMap<>();

    for (Task task : this.taskQueue) {
      for (ArtifactReference reference : task.getCreatedArtifacts()) {
        if (!generatedArtifacts.add(reference)) {
          Set<Task> tasks = duplicatedArtifacts.computeIfAbsent(reference, (k) -> new HashSet<>());

          if (tasks.isEmpty()) {
            tasks.add(taskRegistration.get(reference));
          }

          tasks.add(task);
        } else {
          taskRegistration.put(reference, task);
        }
      }
    }

    if (!duplicatedArtifacts.isEmpty()) {
      StringBuilder message = new StringBuilder("One or more task conflict with each other:");
      message.append(System.lineSeparator());

      duplicatedArtifacts.forEach((a, ts) -> {
        message.append("    * ");
        message.append(a);
        message.append(" provided by ");
        message.append(
            ts.stream()
                .map(Object::getClass)
                .map(Class::getName)
                .collect(Collectors.joining(", "))
        );
        message.append(System.lineSeparator());
      });

      throw new TaskDependencyException(message.toString());
    }

    Set<ArtifactReference> missingArtifacts = this.taskQueue.stream()
        .flatMap((t) -> t.getRequiredArtifacts().stream())
        .filter((a) -> !generatedArtifacts.contains(a))
        .collect(Collectors.toSet());

    if (!missingArtifacts.isEmpty()) {
      StringBuilder message = new StringBuilder("One or more unsatisfied artifact dependencies:");
      message.append(System.lineSeparator());

      missingArtifacts.forEach((a) -> {
        message.append("    * ");
        message.append(a);
        message.append(System.lineSeparator());
      });

      throw new TaskDependencyException(message.toString());
    }
  }

  /**
   * Provides a factory for customized pipeline instances.
   */
  public static class Builder {

    private Cache cache;
    private final Set<Task> tasks = new HashSet<>();
    private final Set<Task.Builder<?>> taskBuilders = new HashSet<>();

    private Builder() {
    }

    /**
     * Constructs a new pipeline instance based on the configuration within this builder.
     *
     * @return a pipeline instance.
     */
    @NonNull
    public Pipeline build() {
      ArtifactManager manager = new ArtifactManager(this.cache);

      Set<Task> tasks = new HashSet<>(this.tasks);
      tasks.addAll(
          this.taskBuilders.stream()
              .map((b) -> b.build(manager))
              .collect(Collectors.toSet())
      );

      return new Pipeline(manager, this.tasks);
    }

    /**
     * Defines a cache implementation which is to be used with the new pipeline.
     *
     * @param cache a cache implementation.
     * @return a reference to this builder instance.
     */
    @NonNull
    public Builder withCache(@NonNull Cache cache) {
      this.cache = cache;
      return this;
    }

    /**
     * Selects a filesystem based cache implementation for the new pipeline.
     *
     * @param directory a cache directory.
     * @return a reference to this builder instance.
     */
    @NonNull
    public Builder withCacheDirectory(@NonNull Path directory) {
      return this.withCache(new LocalFileCache(directory));
    }

    /**
     * Selects an additional task for the new pipeline.
     *
     * @param task a custom task implementation.
     * @return a reference to this builder instance.
     */
    @NonNull
    public Builder withTask(@NonNull Task task) {
      this.tasks.add(task);
      return this;
    }

    /**
     * Selects an additional task for the new pipeline.
     *
     * @param builder a custom task builder implementation.
     * @return a reference to this builder instance.
     */
    @NonNull
    public Builder withTask(@NonNull Task.Builder<?> builder) {
      this.taskBuilders.add(builder);
      return this;
    }

    /**
     * Selects a temporary filesystem based cache implementation for the new pipeline.
     *
     * @return a reference to this builder.
     * @throws IOException when allocating the temporary directory fails.
     */
    @NonNull
    public Builder withTemporaryCache() throws IOException {
      return this.withCache(new TemporaryFileCache());
    }
  }

  /**
   * provides contextual information configured tasks.
   */
  private class ContextImpl implements Context {

    private final Set<Path> temporaryFiles = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path allocateTemporaryFile() throws IOException {
      Path tmp = Files.createTempFile("blackwater_", ".tmp");
      this.temporaryFiles.add(tmp);
      return tmp;
    }

    /**
     * Releases all previously allocated temporary files.
     *
     * @throws IOException when deleting one or more files fails.
     */
    private void releaseTemporaryFiles() throws IOException {
      Map<Path, IOException> exceptions = new HashMap<>();

      for (Path path : this.temporaryFiles) {
        try {
          Files.delete(path);
        } catch (IOException ex) {
          // since we're dealing with temporary files that otherwise have a chance of sticking
          // around forever, we'll simply collect all exceptions in a map and throw a single
          // exception in order to delete as many files as possible
          exceptions.put(path, ex);
        }
      }

      // clear the list of remaining temporary files before throwing any exceptions to keep the
      // context state as clean as possible
      this.temporaryFiles.clear();

      // if there is only a single exception within our map, we'll just wrap and throw it as this is
      // much more caller friendly than our method below
      if (exceptions.size() == 1) {
        IOException ex = exceptions.entrySet().iterator().next().getValue();
        throw new IOException("Failed to delete a temporary file: " + ex.getMessage(), ex);
      }

      // note that this method of handling multiple exceptions isn't exactly pretty, however, it
      // is the preferable option in this case as we would otherwise end up cluttering up the system
      // more than necessary
      // TODO: We can probably remove this once a sufficient amount of Windows installations regularly clear their temporary files
      if (!exceptions.isEmpty()) {
        StringBuilder message = new StringBuilder("Failed to delete multiple temporary files:");
        message.append(System.lineSeparator());

        exceptions.forEach((p, e) -> {
          message.append("    * ");
          message.append(p.toAbsolutePath());
          message.append(" -> ");
          message.append(e.getMessage());
          message.append(System.lineSeparator());

          try (StringWriter writer = new StringWriter()) {
            try (PrintWriter printWriter = new PrintWriter(writer)) {
              e.printStackTrace(printWriter);
            }

            Arrays.stream(writer.toString().split("\n"))
                .map(String::trim)
                .forEach((l) -> {
                  message.append("          ");
                  message.append(l);
                  message.append(System.lineSeparator());
                });
          } catch (IOException ignore) {
          }
        });

        throw new IOException(message.toString());
      }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ArtifactManager getArtifactManager() {
      return Pipeline.this.artifactManager;
    }
  }
}
