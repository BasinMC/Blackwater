package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.basinmc.blackwater.task.Task;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Provides a task which adds arbitrary files within the input directory to the git repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitAddTask implements Task {

  private final Predicate<Path> fileFilter;

  public GitAddTask() {
    this((p) -> true);
  }

  public GitAddTask(@Nonnull Predicate<Path> fileFilter) {
    this.fileFilter = fileFilter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path inputPath = context.getInputPath()
        .orElseThrow(() -> new TaskParameterException("Input path is required"));

    if (inputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Input path cannot be on a custom filesystem");
    }

    if (Files.notExists(inputPath.resolve(".git"))) {
      throw new TaskExecutionException("No repository at input path");
    }

    Path gitBase = inputPath.resolve(".git");
    FileRepositoryBuilder builder = new FileRepositoryBuilder()
        .setWorkTree(inputPath.toFile())
        .setGitDir(gitBase.toFile());

    try (Repository repository = builder.build()) {
      try (Git git = new Git(repository)) {
        AddCommand command = git.add();

        Files.walk(inputPath)
            .filter((p) -> !p.startsWith(gitBase))
            .filter(Files::isRegularFile)
            .sorted(Comparator.comparingInt(Path::getNameCount))
            .forEach((p) -> {
              Path relativePath = inputPath.relativize(p);

              if (!this.fileFilter.test(relativePath)) {
                return;
              }

              command.addFilepattern(relativePath.toString());
            });

        command.call();
      }
    } catch (GitAPIException ex) {
      throw new TaskExecutionException("Failed to add files to repository: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to access repository: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "git-add";
  }
}
