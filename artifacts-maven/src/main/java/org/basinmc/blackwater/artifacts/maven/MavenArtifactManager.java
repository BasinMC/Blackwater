package org.basinmc.blackwater.artifacts.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.basinmc.blackwater.artifact.Artifact;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.utility.CloseableResource;

/**
 * Provides an artifact manager which is backed by a local maven repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MavenArtifactManager implements ArtifactManager {

  private final ArtifactFactory artifactFactory;
  private final ArtifactInstaller artifactInstaller;
  private final ArtifactResolver artifactResolver;
  private final ArtifactRepository localRepository;

  public MavenArtifactManager(
      @Nonnull ArtifactFactory artifactFactory,
      @Nonnull ArtifactInstaller artifactInstaller,
      @Nonnull ArtifactResolver artifactResolver,
      @Nonnull ArtifactRepository localRepository) {
    this.artifactFactory = artifactFactory;
    this.artifactInstaller = artifactInstaller;
    this.artifactResolver = artifactResolver;
    this.localRepository = localRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createArtifact(@Nonnull ArtifactReference reference, @Nonnull Path source)
      throws IOException {
    if (!(reference instanceof MavenArtifactReference)) {
      throw new IllegalArgumentException(
          "Illegal reference: Expected MavenArtifactReference but got " + reference.getClass()
              .getName());
    }

    MavenArtifactReference artifactReference = (MavenArtifactReference) reference;

    if (artifactReference.isDirectory() != Files.isDirectory(source)) {
      throw new IllegalArgumentException(
          "Illegal reference: Reference does not support " + (Files.isDirectory(source)
              ? "directories" : "regular files"));
    }

    // first of all, we'll have to create an artifact for the reference we're dealing with to attach
    // our artifact and models to
    org.apache.maven.artifact.Artifact artifact = this.createMavenArtifact(artifactReference);

    try (CloseableResource<Path, IOException> resource = CloseableResource
        .allocateTemporaryDirectory()) {
      Path outputFile = resource.getResource().resolve("output");

      // now if we're dealing with a directory artifact, we'll have to do copy all contents into a
      // zip archive using the fs api
      if (!artifactReference.isDirectory()) {
        Files.copy(source, outputFile);
      } else {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("create", "true");

        try (FileSystem fs = FileSystems
            .newFileSystem(new URI("jar", outputFile.toUri().toString(), null),
                parameters)) {
          Iterator<Path> it = Files.walk(source, 1)
              .iterator();

          while (it.hasNext()) {
            Path current = it.next();
            Files.copy(current, fs.getPath(source.relativize(current).toString()));
          }
        } catch (URISyntaxException ex) {
          throw new IOException("Illegal input file: " + ex.getMessage(), ex);
        }
      }

      try (CloseableResource<Path, IOException> modelResource = CloseableResource
          .allocateTemporaryFile()) {
        // if the implementation gives us a model, we'll attach it to our artifact before writing it
        // to the repository (for the sake of simplicity we'll simply ignore the temporary file if
        // there is no model to attach)
        Model model = this.createModel(artifactReference).orElse(null);

        if (model != null) {
          try (BufferedWriter writer = Files.newBufferedWriter(modelResource.getResource())) {
            new MavenXpp3Writer().write(writer, model);
          }

          ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
              modelResource.getResource().toFile());
          artifact.addMetadata(metadata);
        }

        // all information has been accumulated thus we may simply write the artifact to the chosen
        // repository
        try {
          this.artifactInstaller
              .install(outputFile.toFile(), artifact, this.localRepository);
        } catch (ArtifactInstallationException ex) {
          throw new IOException("Failed to install artifact: " + ex.getMessage(), ex);
        }
      }
    }
  }

  /**
   * Creates a maven artifact for the specified reference.
   *
   * @param reference a reference.
   * @return an artifact.
   */
  @Nonnull
  protected org.apache.maven.artifact.Artifact createMavenArtifact(
      @Nonnull MavenArtifactReference reference) {
    return this.artifactFactory.createArtifactWithClassifier(
        reference.getGroupId(),
        reference.getArtifactId(),
        reference.getVersion(),
        reference.getPackaging(),
        reference.getClassifier()
    );
  }

  /**
   * Creates a model for an arbitrary artifact.
   *
   * @param reference an artifact reference.
   * @return a model.
   */
  @Nonnull
  protected Optional<Model> createModel(@Nonnull MavenArtifactReference reference) {
    Model model = new Model();
    model.setGroupId(reference.getGroupId());
    model.setArtifactId(reference.getArtifactId());
    model.setVersion(reference.getVersion());
    return Optional.of(model);
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Optional<Artifact> getArtifact(@Nonnull ArtifactReference reference) throws IOException {
    if (!(reference instanceof MavenArtifactReference)) {
      throw new IllegalArgumentException(
          "Illegal reference: Expected MavenArtifactReference but got " + reference.getClass()
              .getName());
    }

    MavenArtifactReference artifactReference = (MavenArtifactReference) reference;

    try {
      org.apache.maven.artifact.Artifact artifact = this.createMavenArtifact(artifactReference);
      this.artifactResolver.resolve(artifact, Collections.emptyList(), this.localRepository);
      return Optional.of(new MavenArtifact(artifactReference, artifact.getFile().toPath()));
    } catch (ArtifactResolutionException ex) {
      throw new IOException("Failed to resolve artifact " + reference + ": " + ex.getMessage());
    } catch (ArtifactNotFoundException ex) {
      return Optional.empty();
    }
  }
}
