package org.basinmc.blackwater.tasks.git;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.task.error.TaskParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a task capable of applying arbitrary patches to a git repository.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class GitApplyMailArchiveTask extends AbstractExecutableTask {

  private static final Logger logger = LoggerFactory.getLogger(GitApplyMailArchiveTask.class);

  private static final Pattern PATCH_FILE_FORMAT = Pattern.compile("^(\\d{4,})-([\\w.-]+)$");

  private final String referenceBranch;

  public GitApplyMailArchiveTask(@Nullable String referenceBranch) {
    this.referenceBranch = referenceBranch;
  }

  public GitApplyMailArchiveTask() {
    this(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    Path inputPath = context.getRequiredInputPath();

    if (inputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Input path cannot be on a custom filesystem");
    }

    Path outputPath = context.getRequiredOutputPath();

    if (Files.notExists(outputPath.resolve(".git"))) {
      throw new TaskExecutionException("No repository at output path");
    }

    if (outputPath.getFileSystem() != FileSystems.getDefault()) {
      throw new TaskParameterException("Output path cannot be on a custom filesystem");
    }

    // before we're trying to apply the mail archive, we'll make sure that no merge is still in
    // progress (either regular or mail archive)
    logger.info("Aborting any pending merge processes ...");
    if (this.execute(new ProcessBuilder("git", "am", "--abort")
        .directory(outputPath.toFile())) != 0) {
      logger.info("  No mail archive merge in progress");
    } else {
      logger.warn("  Mail archive merge aborted");
    }

    if (this.execute(new ProcessBuilder("git", "merge", "--abort")
        .directory(outputPath.toFile())) != 0) {
      logger.info("  No merge in progress");
    } else {
      logger.warn("  Merge aborted");
    }

    // when we were given a reference branch, we'll revert back to it before applying our patches as
    // it acts as an indication that the source code will actually be mostly defined by the patches
    if (this.referenceBranch != null) {
      logger.info("Reverting to reference branch");

      if (this.execute(new ProcessBuilder("git", "reset", "--hard", this.referenceBranch)
          .directory(outputPath.toFile())) != 0) {
        throw new TaskExecutionException(
            "Failed to revert back to reference branch: Git command returned an error");
      }

      logger.info("  Success");
    }

    // since the repository is now in an acceptable state, we'll iterate over the patches in our
    // input directory one by one in their respective order in their originating history (e.g. based
    // on their prefix)
    logger.info("Applying patches ...");

    if (!Files.exists(inputPath)) {
      logger.warn("  Patch directory is missing - Nothing to apply");
      return;
    }

    try {
      Iterator<Path> it = Files.walk(inputPath, 1)
          .filter((p) -> PATCH_FILE_FORMAT.matcher(p.getFileName().toString()).matches())
          .sorted((p1, p2) -> {
            int p1Index = Integer
                .parseUnsignedInt(PATCH_FILE_FORMAT.matcher(p1.getFileName().toString()).group(1));
            int p2Index = Integer
                .parseUnsignedInt(PATCH_FILE_FORMAT.matcher(p2.getFileName().toString()).group(1));

            return Integer.compareUnsigned(p1Index, p2Index);
          })
          .iterator();

      while (it.hasNext()) {
        Path patch = outputPath.relativize(it.next());
        logger.info("  Applying {}", patch.getFileName());

        if (this.execute(
            new ProcessBuilder("git", "am", "--ignore-whitespace", "--3way", patch.toString())
                .directory(outputPath.toFile())) != 0) {
          logger.info("    Failed");
          throw new TaskExecutionException("Failed to apply patch: Git command returned an error");
        }
      }
    } catch (IOException ex) {
      throw new TaskExecutionException("Failed to access patch files: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public String getName() {
    return "git-am";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresInputParameter() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresOutputParameter() {
    return true;
  }
}
