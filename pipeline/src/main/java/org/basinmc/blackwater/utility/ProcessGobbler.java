package org.basinmc.blackwater.utility;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a stream gobbler implementation for processes (e.g. an implementation which redirects
 * any process output to a specific logger).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ProcessGobbler {

  private static final Logger logger = LoggerFactory.getLogger(ProcessGobbler.class);

  private final Thread outputThread;
  private final Thread errorThread;

  public ProcessGobbler(@NonNull Process process, @NonNull Logger logger) {
    this.outputThread = new Thread(() -> this.gobble(logger::info, process.getInputStream()));
    this.errorThread = new Thread(() -> this.gobble(logger::error, process.getErrorStream()));
    this.outputThread.setName("Process");
    this.errorThread.setName("Process");
  }

  @SuppressWarnings("ImplicitDefaultCharsetUsage")
  private void gobble(@NonNull Consumer<String> loggerFunc, @NonNull InputStream inputStream) {
    try (InputStreamReader reader = new InputStreamReader(inputStream)) {
      try (BufferedReader bufferedReader = new BufferedReader(reader)) {
        bufferedReader.lines().forEach((l) -> loggerFunc.accept("    " + l));
      }
    } catch (IOException ex) {
      logger.warn("Failed to redirect process output to logger: " + ex.getMessage());
    }
  }

  /**
   * Starts redirecting the stdout and stderr outputs of the process to the supplied logger.
   */
  public void start() {
    this.outputThread.start();
    this.errorThread.start();
  }
}
