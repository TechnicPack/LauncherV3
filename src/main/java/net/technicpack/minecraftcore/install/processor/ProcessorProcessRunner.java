package net.technicpack.minecraftcore.install.processor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;

/** Executes verified processor artifacts in a separate selected-runtime JVM, not a sandbox. */
public final class ProcessorProcessRunner {
  private static final long CLEANUP_SECONDS = 5;

  public static Path createBootstrapJar(Path workDirectory) throws IOException {
    Path target = Files.createTempFile(workDirectory, "processor-bootstrap-", ".jar");
    boolean complete = false;
    try {
      try (InputStream bytes =
          ProcessorBootstrap.class.getResourceAsStream("ProcessorBootstrap.class")) {
        if (bytes == null) {
          throw new IOException("Missing standalone ProcessorBootstrap.class resource");
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(target))) {
          jar.putNextEntry(
              new JarEntry(ProcessorBootstrap.class.getName().replace('.', '/') + ".class"));
          byte[] buffer = new byte[8192];
          int count;
          while ((count = bytes.read(buffer)) != -1) {
            jar.write(buffer, 0, count);
          }
          jar.closeEntry();
        }
      }
      complete = true;
      return target;
    } finally {
      if (!complete) {
        Files.deleteIfExists(target);
      }
    }
  }

  static Path executablePath(IJavaRuntime runtime) {
    Path executable = runtime.getExecutableFile().toPath().toAbsolutePath().normalize();
    if (OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
        && executable.getFileName().toString().equalsIgnoreCase("javaw.exe")) {
      executable = executable.resolveSibling("java.exe");
    }
    return executable;
  }

  public void run(
      IJavaRuntime runtime,
      Path root,
      Path workDirectory,
      Path bootstrapJar,
      String coordinate,
      List<Path> classpath,
      List<String> arguments,
      BooleanSupplier cancelled)
      throws IOException, InterruptedException {
    Objects.requireNonNull(cancelled, "cancelled");
    checkCancelled(cancelled);
    Path workingRoot = root.toAbsolutePath().normalize();
    Path bootstrap = bootstrapJar.toAbsolutePath().normalize();
    if (!Files.isDirectory(workingRoot) || !Files.isRegularFile(bootstrap)) {
      throw new IOException("Processor root or bootstrap JAR is missing for " + coordinate);
    }
    List<String> entries = new ArrayList<>();
    LinkedHashSet<Path> unique = new LinkedHashSet<>();
    for (Path entry : classpath) {
      Path absolute = entry.toAbsolutePath().normalize();
      if (!Files.isRegularFile(absolute)) {
        throw new IOException(
            "Processor " + coordinate + " classpath is not a regular file: " + absolute);
      }
      if (unique.add(absolute)) {
        entries.add(absolute.toString());
      }
    }
    if (entries.isEmpty()) {
      throw new IOException("Processor " + coordinate + " has no executable JAR");
    }
    String mainClass;
    try (JarFile jar = new JarFile(unique.iterator().next().toFile())) {
      Manifest manifest = jar.getManifest();
      mainClass =
          manifest == null
              ? null
              : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
    }
    if (mainClass == null || mainClass.trim().isEmpty()) {
      throw new IOException("Processor " + coordinate + " has no manifest Main-Class");
    }

    Path executable = executablePath(runtime);
    if (!Files.isRegularFile(executable)) {
      throw new IOException("Selected processor Java executable is missing: " + executable);
    }
    Path executableDirectory = executable.getParent();
    Path javaHome = executableDirectory == null ? null : executableDirectory.getParent();
    if (javaHome == null) {
      throw new IOException("Cannot determine selected Java runtime root from " + executable);
    }

    Path descriptor = Files.createTempFile(workDirectory, "processor-", ".descriptor");
    Path completion = null;
    Throwable failure = null;
    try {
      completion = Files.createTempFile(workDirectory, "processor-", ".complete");
      Files.delete(completion);
      writeDescriptor(descriptor, mainClass.trim(), entries, arguments);
      ProcessBuilder builder =
          ProcessUtils.createProcessBuilder(
              Arrays.asList(
                  executable.toString(),
                  "-cp",
                  bootstrap.toString(),
                  ProcessorBootstrap.class.getName(),
                  descriptor.toAbsolutePath().toString(),
                  completion.toAbsolutePath().toString()));
      builder.directory(workingRoot.toFile()).redirectErrorStream(true);
      Map<String, String> environment = builder.environment();
      environment.put("JAVA_HOME", javaHome.toString());
      String pathKey = "PATH";
      if (OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS) {
        for (String key : environment.keySet()) {
          if (key.equalsIgnoreCase("PATH")) {
            pathKey = key;
            break;
          }
        }
      }
      String inheritedPath = environment.get(pathKey);
      environment.put(
          pathKey,
          executableDirectory
              + (inheritedPath == null || inheritedPath.isEmpty()
                  ? ""
                  : File.pathSeparator + inheritedPath));
      checkCancelled(cancelled);
      Process process = builder.start();
      OutputCapture output = new OutputCapture(process.getInputStream(), coordinate);
      InterruptedException interruption = null;
      int exit;
      try {
        output.reader.start();
        // No processor input is supplied; do not leave tools waiting for stdin.
        closeAsync(process.getOutputStream());
        do {
          checkCancelled(cancelled);
        } while (!process.waitFor(100, TimeUnit.MILLISECONDS));
        checkCancelled(cancelled);
        exit = process.exitValue();
      } catch (InterruptedException cancelledFailure) {
        interruption = cancelledFailure;
        throw cancelledFailure;
      } finally {
        boolean cleanupInterrupted = Thread.interrupted();
        cleanupInterrupted |= cleanup(process, output.reader);
        if (interruption != null && process.isAlive()) {
          interruption.addSuppressed(
              output.failure("child did not terminate after bounded cleanup"));
        }
        if (interruption != null || cleanupInterrupted) {
          Thread.currentThread().interrupt();
        }
        if (cleanupInterrupted && interruption == null) {
          throw new InterruptedException("Processor cleanup interrupted: " + coordinate);
        }
      }
      if (exit != 0) {
        throw output.failure("exited with code " + exit);
      }
      if (!hasCompletion(completion)) {
        throw output.failure("exited without normal processor completion");
      }
      checkCancelled(cancelled);
    } catch (IOException | InterruptedException | RuntimeException | Error problem) {
      failure = problem;
      throw problem;
    } finally {
      IOException deletionFailure = null;
      try {
        Files.deleteIfExists(descriptor);
      } catch (IOException problem) {
        deletionFailure = problem;
      }
      if (completion != null) {
        try {
          Files.deleteIfExists(completion);
        } catch (IOException problem) {
          if (deletionFailure == null) deletionFailure = problem;
          else deletionFailure.addSuppressed(problem);
        }
      }
      if (deletionFailure != null) {
        if (failure == null) throw deletionFailure;
        failure.addSuppressed(deletionFailure);
      }
    }
  }

  private static void writeDescriptor(
      Path descriptor, String mainClass, List<String> classpath, List<String> arguments)
      throws IOException {
    try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(descriptor))) {
      output.writeInt(ProcessorBootstrap.MAGIC);
      output.writeInt(ProcessorBootstrap.VERSION);
      writeString(output, mainClass);
      writeList(output, classpath);
      writeList(output, arguments);
    }
  }

  private static void writeList(DataOutputStream output, List<String> values) throws IOException {
    if (values.size() > ProcessorBootstrap.MAX_LIST_ENTRIES) {
      throw new IOException("Processor descriptor list exceeds entry limit");
    }
    requireSpace(output, 4);
    output.writeInt(values.size());
    for (String value : values) {
      writeString(output, value);
    }
  }

  private static void writeString(DataOutputStream output, String value) throws IOException {
    if (value.length() > ProcessorBootstrap.MAX_STRING_BYTES) {
      throw new IOException("Processor descriptor string exceeds size limit");
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > ProcessorBootstrap.MAX_STRING_BYTES) {
      throw new IOException("Processor descriptor string exceeds size limit");
    }
    requireSpace(output, 4 + bytes.length);
    output.writeInt(bytes.length);
    output.write(bytes);
  }

  private static void requireSpace(DataOutputStream output, int bytes) throws IOException {
    if (bytes > ProcessorBootstrap.MAX_DESCRIPTOR_BYTES - output.size()) {
      throw new IOException("Processor descriptor exceeds size limit");
    }
  }

  private static boolean hasCompletion(Path completion) throws IOException {
    if (!Files.isRegularFile(completion, LinkOption.NOFOLLOW_LINKS)
        || Files.size(completion) != 8) {
      return false;
    }
    try (DataInputStream input = new DataInputStream(Files.newInputStream(completion))) {
      return input.readInt() == ProcessorBootstrap.MAGIC
          && input.readInt() == ProcessorBootstrap.VERSION
          && input.read() == -1;
    }
  }

  private static void checkCancelled(BooleanSupplier cancelled) throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new InterruptedException("Processor execution cancelled");
    }
  }

  /** All potentially blocking pipe operations live on daemon threads, never on the installer. */
  private static boolean cleanup(Process process, Thread reader) {
    boolean interrupted = false;
    if (process.isAlive()) {
      daemon("processor-destroy", process::destroy);
      interrupted |= waitForExit(process);
      if (process.isAlive()) {
        daemon("processor-force-destroy", process::destroyForcibly);
        interrupted |= waitForExit(process);
      }
    }
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLEANUP_SECONDS);
    while (reader.isAlive()) {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) break;
      try {
        TimeUnit.NANOSECONDS.timedJoin(reader, remaining);
      } catch (InterruptedException ignored) {
        interrupted = true;
      }
    }
    // Closing a BufferedReader while another thread is in readLine can block on its monitor.
    // Even raw Process streams can have platform-specific locks: close each independently.
    closeAsync(process.getOutputStream());
    closeAsync(process.getInputStream());
    closeAsync(process.getErrorStream());
    return interrupted;
  }

  private static boolean waitForExit(Process process) {
    boolean interrupted = false;
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLEANUP_SECONDS);
    while (process.isAlive()) {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) break;
      try {
        if (process.waitFor(remaining, TimeUnit.NANOSECONDS)) break;
      } catch (InterruptedException ignored) {
        interrupted = true;
      }
    }
    return interrupted;
  }

  private static void closeAsync(Closeable stream) {
    daemon(
        "processor-close-stream",
        () -> {
          try {
            stream.close();
          } catch (IOException ignored) {
            // The child has already terminated or bounded termination has been attempted.
          }
        });
  }

  private static void daemon(String name, Runnable action) {
    Thread thread = new Thread(action, name);
    thread.setDaemon(true);
    thread.start();
  }

  private static final class OutputCapture {
    private final String coordinate;
    private final ArrayDeque<String> recent = new ArrayDeque<>(50);
    private final Thread reader;

    private OutputCapture(InputStream stream, String coordinate) {
      this.coordinate = coordinate;
      reader =
          new Thread(
              () -> {
                try (BufferedReader lines =
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = lines.readLine()) != null) {
                    synchronized (recent) {
                      if (recent.size() == 50) recent.removeFirst();
                      recent.addLast(line);
                    }
                    Utils.getLogger().info("[" + coordinate + "] " + line);
                  }
                } catch (IOException ignored) {
                  // Stream closure during cancellation/termination is expected.
                }
              },
              "processor-output");
      reader.setDaemon(true);
    }

    private IOException failure(String reason) {
      StringBuilder message =
          new StringBuilder("Processor ").append(coordinate).append(' ').append(reason);
      synchronized (recent) {
        for (String line : recent) {
          message.append('\n').append('[').append(coordinate).append("] ").append(line);
        }
      }
      return new IOException(message.toString());
    }
  }
}
