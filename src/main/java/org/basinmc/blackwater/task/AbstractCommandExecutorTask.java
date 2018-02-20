package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.error.TaskExecutionException;
import org.basinmc.blackwater.utility.ProcessGobbler;
import org.slf4j.LoggerFactory;

/**
 * Provides a task which relies upon external executables to fulfill its purpose.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractCommandExecutorTask extends AbstractConfigurableTask {

  protected AbstractCommandExecutorTask(@NonNull Set<Class<? extends Task>> optionalTasks,
      @NonNull Set<ArtifactReference> requiredArtifacts,
      @NonNull Set<Class<? extends Task>> requiredTasks) {
    super(optionalTasks, requiredArtifacts, requiredTasks);
  }

  /**
   * Executes a specific command within the specified working directory.
   *
   * @param workingDirectory a working directory.
   * @param command a relative or absolute path to an executable or the name of an executable within
   * the system's PATH variable.
   * @param arguments an array of additional arguments to pass.
   * @throws InterruptedException when the process is interrupted while waiting for the process to
   * exit.
   * @throws IOException when invoking the command itself fails.
   * @throws TaskExecutionException when the process exits with a non-zero status code.
   */
  protected void executeCommand(
      @NonNull Path workingDirectory,
      @NonNull String command,
      @NonNull String... arguments)
      throws InterruptedException, IOException, TaskExecutionException {
    List<String> commandList = new ArrayList<>();
    commandList.add(command);
    commandList.addAll(Arrays.asList(arguments));

    Process process = new ProcessBuilder(commandList)
        .directory(workingDirectory.toFile())
        .start();

    ProcessGobbler gobbler = new ProcessGobbler(process, LoggerFactory.getLogger(this.getClass()));

    int exitValue = process.waitFor();
    if (exitValue != 0) {
      throw new TaskExecutionException(
          "Command execution failed: Expected exit value 0 but got " + exitValue);
    }
  }
}
