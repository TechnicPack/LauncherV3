package net.technicpack.minecraftcore.launch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;

/** Runs window discovery independently of the launcher's selected close action. */
public final class WindowsWindowMaximizerLauncher {
  private static final String HELPER_ARGUMENT = "--technic-window-maximizer";
  private static final byte[] READY = "READY\n".getBytes(StandardCharsets.US_ASCII);
  private static final long ATTACH_TIMEOUT_MILLIS = 5000;

  private WindowsWindowMaximizerLauncher() {}

  /** Dispatch before launcher argument parsing, networking, logging, or UI initialization. */
  public static boolean runIfRequested(String[] args) {
    if (args.length == 0 || !HELPER_ARGUMENT.equals(args[0])) {
      return false;
    }
    try {
      if (args.length != 4 || !("true".equals(args[3]) || "false".equals(args[3]))) {
        throw new IllegalArgumentException("Expected PID, creation time, and descendant flag");
      }
      long pid = Long.parseLong(args[1]);
      long creationTime = Long.parseLong(args[2]);
      if (pid <= 0 || pid > 0xffffffffL || creationTime <= 0) {
        throw new IllegalArgumentException("Invalid process identity");
      }
      if (OperatingSystem.getOperatingSystem() != OperatingSystem.WINDOWS) {
        throw new IllegalStateException("Window maximization is only available on Windows");
      }
      try (WindowsWindowMaximizer.Target root = WindowsWindowMaximizer.open(pid, creationTime)) {
        System.out.print("READY\n");
        System.out.flush();
        boolean outputFailed = System.out.checkError();
        System.out.close();
        if (!outputFailed) {
          WindowsWindowMaximizer.run(root, Boolean.parseBoolean(args[3]));
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException | LinkageError e) {
      System.err.println("Window maximizer helper failed: " + e.getMessage());
      e.printStackTrace(System.err);
    }
    return true;
  }

  static void watch(Process process, boolean followDescendants, File diagnosticLog) {
    // Retain the native process identity until the helper has opened its own handle.
    try (WindowsWindowMaximizer.Target root = WindowsWindowMaximizer.open(process)) {
      List<String> command =
          buildCommand(
              new File(Relauncher.getRunningPath(LauncherMain.class)),
              OperatingSystem.getJavaDir(),
              System.getProperty("java.class.path"),
              root.getPid(),
              root.getCreationTime(),
              followDescendants);
      Process helper =
          ProcessUtils.createProcessBuilder(command).redirectError(diagnosticLog).start();
      attach(helper, ATTACH_TIMEOUT_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Utils.getLogger().log(Level.WARNING, "Window maximizer attachment interrupted", e);
    } catch (IOException | ReflectiveOperationException | RuntimeException | LinkageError e) {
      Utils.getLogger().log(Level.WARNING, "Could not start the window maximizer helper", e);
    }
  }

  private static List<String> buildCommand(
      File runningPath,
      String javaExecutable,
      String runtimeClasspath,
      long pid,
      long creationTime,
      boolean followDescendants) {
    List<String> command = new ArrayList<>();
    // Launch4j embeds the launcher JAR, so both packages can use the current JVM.
    command.add(javaExecutable);
    command.add("-Xms8m");
    command.add("-Xmx64m");
    command.add("-cp");
    command.add(runningPath.isDirectory() ? runtimeClasspath : runningPath.getPath());
    command.add(LauncherMain.class.getName());
    command.add(HELPER_ARGUMENT);
    command.add(Long.toString(pid));
    command.add(Long.toString(creationTime));
    command.add(Boolean.toString(followDescendants));
    return command;
  }

  static void attach(Process helper, long timeoutMillis) throws IOException, InterruptedException {
    boolean attached = false;
    try {
      helper.getOutputStream().close();
      awaitReady(helper, timeoutMillis);
      attached = true;
    } finally {
      if (!attached) {
        helper.destroyForcibly();
      }
      closeQuietly(helper.getOutputStream());
      closeQuietly(helper.getInputStream());
      closeQuietly(helper.getErrorStream());
    }
  }

  private static void awaitReady(Process helper, long timeoutMillis)
      throws IOException, InterruptedException {
    InputStream output = helper.getInputStream();
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    int matched = 0;
    while (matched < READY.length) {
      if (Thread.interrupted()) {
        throw new InterruptedException("Window maximizer attachment interrupted");
      }
      // Only read bytes already available: no reader thread or blocking read can outlive
      // the bounded handshake. The helper closes stdout immediately after READY.
      if (output.available() > 0) {
        if (output.read() != READY[matched++]) {
          throw new IOException("Invalid window maximizer handshake");
        }
        continue;
      }
      if (!helper.isAlive()) {
        throw new IOException("Window maximizer helper exited before attaching");
      }
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        throw new IOException("Timed out attaching the window maximizer helper");
      }
      TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(25)));
    }
  }

  private static void closeQuietly(Closeable stream) {
    try {
      stream.close();
    } catch (IOException ignored) {
      // The helper may already have exited or closed its end of the pipe.
    }
  }
}
