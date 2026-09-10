package net.technicpack.utilslib;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ProcessUtils {
  private static final String[] ENV_VARS_TO_REMOVE =
      new String[] {
        "JAVA_ARGS",
        "CLASSPATH",
        "CONFIGPATH",
        "JAVA_HOME",
        "JRE_HOME",
        "_JAVA_OPTIONS",
        "JAVA_OPTIONS",
        "JAVA_TOOL_OPTIONS",
        "JDK_JAVA_OPTIONS",
      };

  private ProcessUtils() {
    // Prevent instantiation of this utility class
  }

  /**
   * Creates a ProcessBuilder with the specified commands and removes certain environment variables
   * that may interfere with the process execution.
   *
   * @param command the list of commands to execute
   * @return a ProcessBuilder configured with the provided commands and cleaned environment
   *     variables
   */
  public static ProcessBuilder createProcessBuilder(List<String> command) {
    ProcessBuilder pb = new ProcessBuilder(command);
    Map<String, String> env = pb.environment();

    for (String varName : ENV_VARS_TO_REMOVE) {
      env.remove(varName);
    }

    return pb;
  }

  public static ProcessBuilder createProcessBuilder(List<String> command, boolean allowJavaHome) {
    ProcessBuilder pb = new ProcessBuilder(command);
    Map<String, String> env = pb.environment();

    for (String varName : ENV_VARS_TO_REMOVE) {
      if (varName.equals("JAVA_HOME") && allowJavaHome) {
        continue;
      }
      env.remove(varName);
    }

    return pb;
  }

  /**
   * @see #createProcessBuilder(List)
   */
  public static ProcessBuilder createProcessBuilder(String... command) {
    return createProcessBuilder(Arrays.asList(command));
  }

  /** Capture merged stdout/stderr without discarding the exit code or process startup failure. */
  public static ProcessOutput captureOutput(String... command)
      throws IOException, InterruptedException {
    Process process = createProcessBuilder(command).redirectErrorStream(true).start();
    FutureTask<String> output =
        new FutureTask<>(
            () -> {
              StringBuilder response = new StringBuilder();
              try (Reader reader =
                  new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                  response.append(buffer, 0, length);
                }
              }
              return response.toString();
            });
    Thread reader = new Thread(output, "process-output");
    reader.setDaemon(true);
    try {
      reader.start();
      int exitCode = process.waitFor();
      return new ProcessOutput(exitCode, output.get());
    } catch (ExecutionException e) {
      throw new IOException(
          "Error reading process output: " + String.join(" ", command), e.getCause());
    } finally {
      process.destroy();
    }
  }

  public static final class ProcessOutput {
    private final int exitCode;
    private final String output;

    private ProcessOutput(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }

    public int getExitCode() {
      return exitCode;
    }

    public String getOutput() {
      return output;
    }
  }
}
