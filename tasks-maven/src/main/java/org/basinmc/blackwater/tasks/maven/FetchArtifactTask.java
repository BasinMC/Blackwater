package org.basinmc.blackwater.tasks.maven;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
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
  private final List<ArtifactRepository> repositories;

  private final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();

  public FetchArtifactTask(
      @Nonnull DependencyResolver resolver,
      @Nonnull ProjectBuildingRequest buildingRequest,
      @Nonnull List<ArtifactRepository> repositories,
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nonnull String type,
      @Nullable String classifier) {
    this.resolver = resolver;
    this.buildingRequest = buildingRequest;
    this.repositories = new ArrayList<>(repositories);

    this.coordinate.setGroupId(groupId);
    this.coordinate.setArtifactId(artifactId);
    this.coordinate.setVersion(version);
    this.coordinate.setType(type);
    this.coordinate.setClassifier(classifier);
  }

  public FetchArtifactTask(
      @Nonnull DependencyResolver resolver,
      @Nonnull ProjectBuildingRequest buildingRequest,
      @Nonnull List<ArtifactRepository> repositories,
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version,
      @Nonnull String type) {
    this(resolver, buildingRequest, repositories, groupId, artifactId, version, type, null);
  }

  public FetchArtifactTask(
      @Nonnull DependencyResolver resolver,
      @Nonnull ProjectBuildingRequest buildingRequest,
      @Nonnull List<ArtifactRepository> repositories,
      @Nonnull String groupId,
      @Nonnull String artifactId,
      @Nonnull String version) {
    this(resolver, buildingRequest, repositories, groupId, artifactId, version, "jar", null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    ProjectBuildingRequest request = new DefaultProjectBuildingRequest(this.buildingRequest);
    request.setRemoteRepositories(this.repositories);

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
