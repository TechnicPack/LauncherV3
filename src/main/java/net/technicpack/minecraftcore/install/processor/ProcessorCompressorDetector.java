package net.technicpack.minecraftcore.install.processor;

import com.sun.jna.Library;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.utilslib.Utils;

/**
 * Best-effort inspection of the selected processor JVM; unknown results always retain hash checks.
 */
public final class ProcessorCompressorDetector {
  private static final long PROBE_SECONDS = 10;
  private static final int MAX_OUTPUT_BYTES = 16 * 1024;
  private static final String POSITIVE_RESULT = "TECHNIC_PROCESSOR_COMPRESSOR=ZLIB_NG\n";

  private ProcessorCompressorDetector() {}

  public static boolean usesZlibNg(IJavaRuntime runtime, BooleanSupplier cancelled)
      throws InterruptedException {
    Objects.requireNonNull(runtime, "runtime");
    Objects.requireNonNull(cancelled, "cancelled");
    ProcessorProcessRunner.checkCancelled(cancelled);
    if (!"Linux".equals(System.getProperty("os.name"))) return false;
    Path work = null;
    try {
      work = Files.createTempDirectory("technic-compressor-");
      Path helper = createProbeJar(work);
      // In development JNA is a dependency JAR; in a packaged launcher it is in the shaded JAR.
      // Do not inherit java.class.path (Gradle workers in particular do not expose their
      // dependencies
      // there). The processor's own isolated classpath is never changed by this separate probe.
      String classpath = helper + File.pathSeparator + jnaLocation();
      ProcessBuilder builder =
          ProcessorProcessRunner.createProcessBuilder(
              runtime,
              Arrays.asList(
                  ProcessorProcessRunner.executablePath(runtime).toString(),
                  "-Djava.io.tmpdir=" + work,
                  "-Djna.tmpdir=" + work,
                  "-Djna.nosys=true",
                  "-cp",
                  classpath,
                  ProcessorCompressorProbe.class.getName()));
      builder.directory(work.toFile()).redirectErrorStream(true);
      boolean detected = runProbe(builder, cancelled);
      ProcessorProcessRunner.checkCancelled(cancelled);
      return detected;
    } catch (IOException | URISyntaxException | RuntimeException | LinkageError failure) {
      ProcessorProcessRunner.checkCancelled(cancelled);
      Utils.getLogger()
          .log(
              Level.WARNING,
              "Cannot identify selected JVM compressor; retaining hash checks",
              failure);
      return false;
    } finally {
      if (work != null) {
        try {
          Files.walkFileTree(
              work,
              new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                  Files.deleteIfExists(file);
                  return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                    throws IOException {
                  if (failure != null) throw failure;
                  Files.deleteIfExists(directory);
                  return FileVisitResult.CONTINUE;
                }
              });
        } catch (IOException | RuntimeException failure) {
          Utils.getLogger()
              .log(Level.WARNING, "Cannot remove compressor probe files: " + work, failure);
        }
      }
      ProcessorProcessRunner.checkCancelled(cancelled);
    }
  }

  private static Path jnaLocation() throws IOException, URISyntaxException {
    CodeSource source = Library.class.getProtectionDomain().getCodeSource();
    if (source == null || !"file".equals(source.getLocation().getProtocol())) {
      throw new IOException("Cannot locate the JNA dependency for the selected JVM");
    }
    Path location = Paths.get(source.getLocation().toURI()).toAbsolutePath().normalize();
    if (!Files.isRegularFile(location) && !Files.isDirectory(location)) {
      throw new IOException("JNA dependency is unavailable: " + location);
    }
    return location;
  }

  private static Path createProbeJar(Path work) throws IOException {
    Path helper = work.resolve("compressor-probe.jar");
    try (InputStream bytes =
        ProcessorCompressorProbe.class.getResourceAsStream("ProcessorCompressorProbe.class")) {
      if (bytes == null) {
        throw new IOException("Missing standalone ProcessorCompressorProbe.class resource");
      }
      try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(helper))) {
        jar.putNextEntry(
            new JarEntry(ProcessorCompressorProbe.class.getName().replace('.', '/') + ".class"));
        byte[] buffer = new byte[8192];
        int count;
        while ((count = bytes.read(buffer)) != -1) {
          jar.write(buffer, 0, count);
        }
        jar.closeEntry();
      }
    }
    return helper;
  }

  private static boolean runProbe(ProcessBuilder builder, BooleanSupplier cancelled)
      throws IOException, InterruptedException {
    ProcessorProcessRunner.checkCancelled(cancelled);
    Process process = builder.start();
    ProbeOutput output = new ProbeOutput(process.getInputStream());
    InterruptedException interruption = null;
    boolean exited = false;
    try {
      output.reader.start();
      ProcessorProcessRunner.closeAsync(process.getOutputStream());
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(PROBE_SECONDS);
      do {
        ProcessorProcessRunner.checkCancelled(cancelled);
        exited = process.waitFor(100, TimeUnit.MILLISECONDS);
      } while (!exited && System.nanoTime() < deadline);
      ProcessorProcessRunner.checkCancelled(cancelled);
    } catch (InterruptedException failure) {
      interruption = failure;
      throw failure;
    } finally {
      boolean cleanupInterrupted = Thread.interrupted();
      cleanupInterrupted |= ProcessorProcessRunner.cleanup(process, output.reader);
      if (interruption != null || cleanupInterrupted) {
        Thread.currentThread().interrupt();
      }
      if (cleanupInterrupted && interruption == null) {
        throw new InterruptedException("Compressor probe cleanup interrupted");
      }
    }
    ProcessorProcessRunner.checkCancelled(cancelled);
    String diagnostics = output.text();
    if (!exited || process.isAlive() || process.exitValue() != 0 || !output.complete()) {
      Utils.getLogger()
          .warning(
              "Selected JVM compressor probe failed or timed out; retaining hash checks\n"
                  + diagnostics);
      return false;
    }
    Utils.getLogger().info("Selected JVM compressor probe:\n" + diagnostics);
    return diagnostics.equals(POSITIVE_RESULT) || diagnostics.endsWith("\n" + POSITIVE_RESULT);
  }

  /**
   * Drain pipes without letting even a faulty JVM allocate unbounded output or block the installer.
   */
  private static final class ProbeOutput {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final Thread reader;
    private boolean ended;
    private boolean overflow;

    private ProbeOutput(InputStream stream) {
      reader =
          new Thread(
              () -> {
                try (InputStream input = stream) {
                  byte[] buffer = new byte[4096];
                  int count;
                  while ((count = input.read(buffer)) != -1) {
                    synchronized (this) {
                      int accepted = Math.min(count, MAX_OUTPUT_BYTES - bytes.size());
                      bytes.write(buffer, 0, accepted);
                      if (accepted != count) overflow = true;
                    }
                  }
                  synchronized (this) {
                    ended = true;
                  }
                } catch (IOException ignored) {
                  // Incomplete capture cannot establish a positive result.
                }
              },
              "compressor-probe-output");
      reader.setDaemon(true);
    }

    private synchronized String text() {
      return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private synchronized boolean complete() {
      return ended && !overflow;
    }
  }
}
