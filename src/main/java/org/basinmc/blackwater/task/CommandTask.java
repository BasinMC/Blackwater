package org.basinmc.blackwater.task;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.basinmc.blackwater.artifact.ArtifactManager;
import org.basinmc.blackwater.artifact.ArtifactReference;
import org.basinmc.blackwater.task.error.TaskExecutionException;

/**
 * Provides a task which invokes an arbitrary executable.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CommandTask extends AbstractConfigurableTask {

  private final List<String> command = new ArrayList<>();
  private final Path workingDirectory;
  private final Map<String, String> environmentVariables = new HashMap<>();
  private final Function<Integer, TaskExecutionException> exitValueFunction;

  protected CommandTask(
      @NonNull List<String> command,
      @NonNull Path workingDirectory,
      @NonNull Map<String, String> environmentVariables,
      @NonNull Function<Integer, TaskExecutionException> exitValueFunction,
      @NonNull Set<Class<? extends Task>> optionalTasks,
      @NonNull Set<ArtifactReference> requiredArtifacts,
      @NonNull Set<Class<? extends Task>> requiredTasks) {
    super(optionalTasks, requiredArtifacts, requiredTasks);
    this.command.addAll(command);
    this.workingDirectory = workingDirectory;
    this.environmentVariables.putAll(environmentVariables);
    this.exitValueFunction = exitValueFunction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(@NonNull Context context) throws TaskExecutionException {
    ProcessBuilder builder = new ProcessBuilder(this.command)
        .directory(this.workingDirectory.toFile());

    Map<String, String> environmentVariables = builder.environment();
    environmentVariables.putAll(this.environmentVariables);

    try {
      int exitValue = builder.start().waitFor();
      TaskExecutionException exception = this.exitValueFunction.apply(exitValue);

      if (exception != null) {
        throw exception;
      }
    } catch (InterruptedException | IOException ex) {
      throw new TaskExecutionException("Failed to invoke command: " + ex.getMessage(), ex);
    }
  }

  /**
   * Provides a factory for command task instances.
   */
  public static class Builder extends AbstractConfigurableTask.Builder<CommandTask> {

    private String command;
    private final List<String> arguments = new ArrayList<>();
    private Path workingDirectory = Paths.get(".");
    private final Map<String, String> environmentVariables = new HashMap<>();
    private Function<Integer, TaskExecutionException> exitValueFunction = (v) -> {
      if (v != 0) {
        return new TaskExecutionException("Expected return code 0 but got " + v);
      }

      return null;
    };

    /**
     * Constructs a new command task based on the configuration of this builder.
     *
     * @return a new task.
     * @throws IllegalStateException when no command was specified.
     */
    @NonNull
    public CommandTask build() {
      if (this.command == null) {
        throw new IllegalStateException("Illegal task configuration: Command is required");
      }

      List<String> command = new ArrayList<>();
      command.add(this.command);
      command.addAll(this.arguments);

      return new CommandTask(
          command,
          this.workingDirectory,
          this.environmentVariables,
          this.exitValueFunction,
          this.optionalTasks,
          this.requiredArtifacts,
          this.requiredTasks
      );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public CommandTask build(@NonNull ArtifactManager manager) {
      return this.build();
    }

    /**
     * Appends an argument to the command.
     *
     * @param argument a command line argument.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withArgument(@NonNull String argument) {
      this.arguments.add(argument);
      return this;
    }

    /**
     * Selects a command to execute.
     *
     * @param command a relative or absolute path to an executable or the name of an executable
     * which is found within the system PATH variable.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withCommand(@NonNull String command) {
      this.command = command;
      return this;
    }

    /**
     * Appends an environment variable to pass to the command execution environment.
     *
     * @param name a variable name.
     * @param value an environment value.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withEnvironmentVariable(@NonNull String name, @NonNull String value) {
      this.environmentVariables.put(name, value);
      return this;
    }

    /**
     * Selects an exit value processor function which evaluates whether or not the execution shall
     * be considered a failure (a return value of null will indicate a success, any other value will
     * be considered a failure).
     *
     * @param function a exit value processor function.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withExitValueFunction(
        @NonNull Function<Integer, TaskExecutionException> function) {
      this.exitValueFunction = function;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withOptionalTask(@NonNull Class<? extends Task> task) {
      super.withOptionalTask(task);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withRequiredArtifact(@NonNull ArtifactReference reference) {
      super.withRequiredArtifact(reference);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withRequiredTask(@NonNull Class<? extends Task> task) {
      super.withRequiredTask(task);
      return this;
    }

    /**
     * Selects a working directory to invoke the command in.
     *
     * @param workingDirectory a working directory (relative or absolute).
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withWorkingDirectory(@NonNull Path workingDirectory) {
      this.workingDirectory = workingDirectory;
      return this;
    }
  }
}
