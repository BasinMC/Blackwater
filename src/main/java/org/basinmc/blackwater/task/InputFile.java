package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Objects;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.error.TaskDependencyException;

/**
 * <p>Represents an input file which may be either an artifact (which is to be resolved at execution
 * time) or a specific file.</p>
 *
 * <p>This specification is intended to be used specifically for implementations which wish to
 * accept either an artifact or an external file in a generic way.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface InputFile {

  /**
   * Retrieves a reference to the location at which this input file is available.
   *
   * @return a file reference.
   * @throws TaskDependencyException when the file is not accessible yet.
   */
  @NonNull
  Path getPath() throws TaskDependencyException;

  /**
   * Provides an input file which refers to an artifact which has been generated throughout the
   * pipeline's execution cycle.
   */
  class ArtifactInputFile implements InputFile {

    private final ArtifactManager manager;
    private final ArtifactReference reference;

    public ArtifactInputFile(
        @NonNull ArtifactManager manager,
        @NonNull ArtifactReference reference) {
      this.manager = manager;
      this.reference = reference;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path getPath() throws TaskDependencyException {
      return this.manager.getArtifact(this.reference)
          .orElseThrow(() -> new TaskDependencyException(
              "Required artifact " + this.reference + " is inaccessible"))
          .getPath();
    }

    /**
     * Retrieves a reference for the artifact this input file will refer to.
     *
     * @return an artifact reference.
     */
    @NonNull
    public ArtifactReference getReference() {
      return this.reference;
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
      ArtifactInputFile that = (ArtifactInputFile) o;
      return Objects.equals(this.manager, that.manager) &&
          Objects.equals(this.reference, that.reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.manager, this.reference);
    }
  }

  /**
   * Provides an input file which refers to a standard NIO path (such as a file on a local file
   * system).
   */
  class NioInputFile implements InputFile {

    private final Path path;

    public NioInputFile(Path path) {
      this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Path getPath() {
      return this.path;
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
      NioInputFile that = (NioInputFile) o;
      return Objects.equals(this.path, that.path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.path);
    }
  }
}
