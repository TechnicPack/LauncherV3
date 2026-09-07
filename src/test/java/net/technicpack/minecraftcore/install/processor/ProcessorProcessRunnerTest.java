package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import net.technicpack.launchercore.launch.java.version.CurrentJavaRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProcessorProcessRunnerTest {
  @TempDir Path temp;
  private Path root;
  private Path work;
  private Path bootstrap;
  private Path processor;
  private final ProcessorProcessRunner runner = new ProcessorProcessRunner();

  @BeforeEach
  void packageActualChild() throws Exception {
    root = Files.createDirectories(temp.resolve("launcher root"));
    work = Files.createDirectories(root.resolve("cache/work"));
    bootstrap = ProcessorProcessRunner.createBootstrapJar(work);
    processor = processorJar("processor.jar", ProcessorTestFixture.class.getName(), null);
  }

  @Test
  void descriptorPreservesArgumentBoundariesAndBypassesOperatingSystemCommandLength()
      throws Exception {
    Path captured = root.resolve("captured.bin");
    List<String> expected =
        Arrays.asList(
            "",
            "two words",
            "'quoted'",
            "\"double\"",
            "back\\slash",
            "line\nbreak",
            "Grüße 雪 \uD83D\uDE80",
            "{unexpanded}",
            "a".repeat(1024 * 1024));
    List<String> arguments = new ArrayList<>(Arrays.asList("capture", captured.toString()));
    arguments.addAll(expected);
    run(arguments);
    try (DataInputStream input = new DataInputStream(Files.newInputStream(captured))) {
      assertEquals(expected.size(), input.readInt());
      for (String value : expected) {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        assertEquals(value, new String(bytes, StandardCharsets.UTF_8));
      }
      assertEquals(-1, input.read());
    }
  }

  @Test
  void processorSeesOnlyItsOrderedDeduplicatedClasspathAndSelectedRuntimeEnvironment()
      throws Exception {
    Path first = processorJar("first.jar", null, "first");
    Path second = processorJar("second.jar", null, "second");
    Path captured = root.resolve("isolation.bin");
    runner.run(
        new CurrentJavaRuntime(),
        root,
        work,
        bootstrap,
        "fixture:isolation:1",
        Arrays.asList(processor, first, second, first, processor),
        Arrays.asList("isolation", captured.toString()),
        () -> false);
    try (DataInputStream input = new DataInputStream(Files.newInputStream(captured))) {
      assertEquals(root.toRealPath(), Paths.get(input.readUTF()).toRealPath());
      assertEquals(
          Paths.get(System.getProperty("java.home")).toRealPath(),
          Paths.get(input.readUTF()).toRealPath());
      assertEquals(
          Paths.get(System.getProperty("java.home")).toRealPath(),
          Paths.get(input.readUTF()).toRealPath());
      assertEquals(
          javaExecutable().getParent().toString(),
          input.readUTF().split(java.util.regex.Pattern.quote(java.io.File.pathSeparator), 2)[0]);
      assertEquals(3, input.readInt());
      assertEquals(processor, Paths.get(input.readUTF()));
      assertEquals(first, Paths.get(input.readUTF()));
      assertEquals(second, Paths.get(input.readUTF()));
      assertEquals("first", input.readUTF());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"throw", "nonzero", "early-exit"})
  void failuresAndPrematureZeroExitNeverCountAsNormalCompletion(String mode) {
    IOException failure =
        assertThrows(IOException.class, () -> run(Collections.singletonList(mode)));
    assertTrue(failure.getMessage().contains("fixture:processor:1"));
    if (mode.equals("throw")) {
      assertTrue(failure.getMessage().contains("fixture-thrown-cause"));
      assertTrue(failure.getMessage().contains("diagnostic-59"));
      assertFalse(failure.getMessage().contains("] diagnostic-0\n"));
      assertFalse(failure.getMessage().contains("InvocationTargetException"));
      assertEquals(
          50,
          failure
              .getMessage()
              .lines()
              .filter(line -> line.startsWith("[fixture:processor:1]"))
              .count());
    } else if (mode.equals("nonzero")) {
      assertTrue(failure.getMessage().contains("code 7"));
      assertTrue(failure.getMessage().contains("fixture-nonzero"));
    } else {
      assertTrue(failure.getMessage().contains("without normal processor completion"));
      assertTrue(failure.getMessage().contains("fixture-early-exit"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cancellationAndInterruptionTerminateTheDirectChildAndPreserveInterruptState(
      boolean interrupt) throws Exception {
    Path pidFile = root.resolve("child.pid");
    AtomicBoolean cancelled = new AtomicBoolean();
    AtomicBoolean interruptPreserved = new AtomicBoolean();
    AtomicReference<Thread> worker = new AtomicReference<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    long pid = -1;
    try {
      Future<?> result =
          executor.submit(
              () -> {
                worker.set(Thread.currentThread());
                try {
                  runner.run(
                      new CurrentJavaRuntime(),
                      root,
                      work,
                      bootstrap,
                      "fixture:sleep:1",
                      Collections.singletonList(processor),
                      Arrays.asList("sleep", pidFile.toString()),
                      cancelled::get);
                } catch (InterruptedException failure) {
                  interruptPreserved.set(Thread.currentThread().isInterrupted());
                  throw failure;
                }
                return null;
              });
      pid = awaitPid(pidFile);
      assertTrue(ProcessHandle.of(pid).orElseThrow().isAlive());
      if (interrupt) worker.get().interrupt();
      else cancelled.set(true);
      ExecutionException failure =
          assertThrows(ExecutionException.class, () -> result.get(20, TimeUnit.SECONDS));
      assertInstanceOf(InterruptedException.class, failure.getCause());
      assertTrue(interruptPreserved.get());
      assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    } finally {
      cancelled.set(true);
      executor.shutdownNow();
      terminate(pid);
      assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
    }
  }

  @Test
  void inheritedOutputPipeCannotKeepInstallerWaitingForADescendant() throws Exception {
    Path pidFile = root.resolve("descendant.pid");
    ExecutorService executor = Executors.newSingleThreadExecutor();
    long pid = -1;
    try {
      Future<?> result =
          executor.submit(
              () -> {
                run(Arrays.asList("inherited-pipe", pidFile.toString()));
                return null;
              });
      pid = awaitPid(pidFile);
      result.get(12, TimeUnit.SECONDS);
      // Java 8 Process deliberately does not promise arbitrary descendant-tree cleanup.
      assertTrue(ProcessHandle.of(pid).orElseThrow().isAlive());
    } finally {
      if (pid == -1 && Files.exists(pidFile)) pid = Long.parseLong(Files.readString(pidFile));
      terminate(pid);
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
    }
  }

  @Test
  void missingDependencyAndMissingManifestFailBeforeProcessorExecution() throws Exception {
    Path captured = root.resolve("must-not-exist.bin");
    assertThrows(
        IOException.class,
        () ->
            runner.run(
                new CurrentJavaRuntime(),
                root,
                work,
                bootstrap,
                "fixture:missing:1",
                Arrays.asList(processor, root.resolve("missing.jar")),
                Arrays.asList("capture", captured.toString()),
                () -> false));
    Path noMain = processorJar("no-main.jar", null, null);
    assertThrows(
        IOException.class,
        () ->
            runner.run(
                new CurrentJavaRuntime(),
                root,
                work,
                bootstrap,
                "fixture:missing-main:1",
                Collections.singletonList(noMain),
                Arrays.asList("capture", captured.toString()),
                () -> false));
    assertFalse(Files.exists(captured));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "magic",
        "version",
        "negative-string",
        "oversized-string",
        "negative-classpath",
        "oversized-classpath",
        "negative-arguments",
        "oversized-arguments",
        "trailing",
        "truncated",
        "oversized-file",
        "invalid-utf8"
      })
  void standaloneBootstrapRejectsInvalidDescriptorsWithoutLeavingACompletionMarker(String defect)
      throws Exception {
    Path captured = root.resolve("invalid-descriptor-must-not-run.bin");
    byte[] valid = descriptor(Arrays.asList("capture", captured.toString()));
    int classpathOffset =
        12 + ProcessorTestFixture.class.getName().getBytes(StandardCharsets.UTF_8).length;
    int argumentsOffset =
        classpathOffset + 8 + processor.toString().getBytes(StandardCharsets.UTF_8).length;
    byte[] damaged = valid.clone();
    ByteBuffer buffer = ByteBuffer.wrap(damaged);
    switch (defect) {
      case "magic":
        buffer.putInt(0, 0);
        break;
      case "version":
        buffer.putInt(4, 2);
        break;
      case "negative-string":
        buffer.putInt(8, -1);
        break;
      case "oversized-string":
        buffer.putInt(8, 1024 * 1024 + 1);
        break;
      case "negative-classpath":
        buffer.putInt(classpathOffset, -1);
        break;
      case "oversized-classpath":
        buffer.putInt(classpathOffset, 65536);
        break;
      case "negative-arguments":
        buffer.putInt(argumentsOffset, -1);
        break;
      case "oversized-arguments":
        buffer.putInt(argumentsOffset, 65536);
        break;
      case "trailing":
        damaged = Arrays.copyOf(valid, valid.length + 1);
        break;
      case "truncated":
        damaged = Arrays.copyOf(valid, valid.length - 1);
        break;
      case "invalid-utf8":
        damaged[12] = (byte) 0xff;
        break;
      case "oversized-file":
        break;
      default:
        throw new AssertionError(defect);
    }
    Path input = work.resolve("invalid.descriptor");
    Files.write(input, damaged);
    if (defect.equals("oversized-file")) {
      try (SeekableByteChannel channel = Files.newByteChannel(input, StandardOpenOption.WRITE)) {
        channel.position(16 * 1024 * 1024);
        channel.write(ByteBuffer.wrap(new byte[] {0}));
      }
    }
    Path marker = work.resolve("stale.complete");
    Files.write(marker, new byte[] {0x54, 0x50, 0x49, 0x50, 0, 0, 0, 1});
    Path log = work.resolve("invalid.log");
    Process child =
        new ProcessBuilder(
                javaExecutable().toString(),
                "-cp",
                bootstrap.toString(),
                ProcessorBootstrap.class.getName(),
                input.toString(),
                marker.toString())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    try {
      assertTrue(child.waitFor(10, TimeUnit.SECONDS), "descriptor probe did not terminate");
      assertNotEquals(0, child.exitValue(), Files.readString(log));
      assertFalse(Files.exists(marker));
      assertFalse(Files.exists(captured));
    } finally {
      if (child.isAlive()) {
        child.destroyForcibly();
        assertTrue(child.waitFor(5, TimeUnit.SECONDS));
      }
    }
  }

  @Test
  void runnerRejectsOverLimitDescriptorsRatherThanLaunchingATruncatedCommand() throws Exception {
    Path captured = root.resolve("over-limit.bin");
    List<String> arguments = new ArrayList<>(Arrays.asList("capture", captured.toString()));
    arguments.add("a".repeat(1024 * 1024 + 1));
    assertThrows(IOException.class, () -> run(arguments));
    arguments.remove(arguments.size() - 1);
    arguments.addAll(Collections.nCopies(16, "a".repeat(1024 * 1024)));
    assertThrows(IOException.class, () -> run(arguments));
    assertThrows(IOException.class, () -> run(Collections.nCopies(65536, "")));
    assertFalse(Files.exists(captured));
  }

  private void run(List<String> arguments) throws IOException, InterruptedException {
    runner.run(
        new CurrentJavaRuntime(),
        root,
        work,
        bootstrap,
        "fixture:processor:1",
        Collections.singletonList(processor),
        arguments,
        () -> false);
  }

  private Path processorJar(String filename, String mainClass, String resource) throws IOException {
    Path jar = root.resolve(filename);
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (mainClass != null) manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
      if (mainClass != null) {
        output.putNextEntry(
            new JarEntry(ProcessorTestFixture.class.getName().replace('.', '/') + ".class"));
        try (InputStream input =
            ProcessorTestFixture.class.getResourceAsStream("ProcessorTestFixture.class")) {
          assertNotNull(input);
          input.transferTo(output);
        }
        output.closeEntry();
      }
      if (resource != null) {
        output.putNextEntry(new JarEntry("classpath-order.txt"));
        output.write(resource.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
      }
    }
    return jar;
  }

  private byte[] descriptor(List<String> arguments) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream output = new DataOutputStream(bytes)) {
      output.writeInt(0x54504950);
      output.writeInt(1);
      writeString(output, ProcessorTestFixture.class.getName());
      output.writeInt(1);
      writeString(output, processor.toString());
      output.writeInt(arguments.size());
      for (String argument : arguments) writeString(output, argument);
    }
    return bytes.toByteArray();
  }

  private static void writeString(DataOutputStream output, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    output.writeInt(bytes.length);
    output.write(bytes);
  }

  private static Path javaExecutable() {
    return Paths.get(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
        .toAbsolutePath()
        .normalize();
  }

  private static long awaitPid(Path file) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (!Files.isRegularFile(file) || Files.size(file) == 0) {
      if (System.nanoTime() >= deadline) fail("Processor did not publish its PID");
      Thread.sleep(20);
    }
    return Long.parseLong(Files.readString(file));
  }

  private static void terminate(long pid) throws Exception {
    if (pid < 0) return;
    ProcessHandle process = ProcessHandle.of(pid).orElse(null);
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
      process.onExit().get(5, TimeUnit.SECONDS);
    }
  }
}
