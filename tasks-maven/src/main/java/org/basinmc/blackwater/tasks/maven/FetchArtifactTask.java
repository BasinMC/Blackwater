package org.basinmc.blackwater.tasks.maven;

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.annotation.Nonnull;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependencies.DependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves and installs an artifact to the local maven repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class FetchArtifactTask implements Task {

  private static final Logger logger = LoggerFactory.getLogger(FetchArtifactTask.class);

  private final DependencyResolver resolver;
  private final ProjectBuildingRequest buildingRequest;
  private final DependableCoordinate coordinate;

  public FetchArtifactTask(
      @Nonnull DependencyResolver resolver,
      @Nonnull ProjectBuildingRequest buildingRequest,
      @NonNull DependableCoordinate coordinate) {
    this.resolver = resolver;
    this.buildingRequest = buildingRequest;
    this.coordinate = coordinate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    try {
      logger.info("Fetching artifact {} and all of its dependencies ...", this.coordinate);
      this.resolver.resolveDependencies(this.buildingRequest, this.coordinate, null);
    } catch (DependencyResolverException ex) {
      throw new TaskExecutionException(
          "Failed to resolve artifact " + this.coordinate + ": " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "maven-fetch-artifact";
  }
}
