package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(45)
class ModernInstallerEngineTest {
  private static final String TOOL = "test.engine:processor:1";
  private static final String GENERATED = "test.engine:generated:1";
  private static final String CACHED_SOURCE = "test.engine:cache-source:1";
  private static final String GOOD = "complete processor output";
  private static final String GOOD_HASH = DigestUtils.sha1Hex(GOOD);
  private static final NodeProgressReporter REPORTER =
      new NodeProgressReporter() {
        public void updateNodeProgress(float percent) {}

        public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {}
      };
  @TempDir Path temp;
  private Path root;
  private byte[] toolJar;

  @BeforeEach
  void createChildJar() throws Exception {
    root = Files.createDirectory(temp.resolve("root"));
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, ChildProcessor.class.getName());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarOutputStream jar = new JarOutputStream(bytes, manifest)) {
      String resource = ChildProcessor.class.getName().replace('.', '/') + ".class";
      jar.putNextEntry(new JarEntry(resource));
      try (InputStream input = ChildProcessor.class.getResourceAsStream("/" + resource)) {
        assertNotNull(input);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) jar.write(buffer, 0, count);
      }
      jar.closeEntry();
    }
    toolJar = bytes.toByteArray();
  }

  @Test
  void allValidOutputsSkipButCorruptionRunsAndRepairs() throws Exception {
    Path output = write(root.resolve("products/final.bin"), GOOD);
    ModernInstallerEngine.Request request =
        request(
            processor(
                "write",
                outputs("products/final.bin", GOOD_HASH),
                "{ROOT}/products/final.bin",
                GOOD));
    execute(request);
    assertFalse(Files.exists(root.resolve("invocations.log")));
    write(output, "corrupted");
    execute(request);
    assertEquals(GOOD, text(output));
    assertEquals(Collections.singletonList("write"), invocations());
    execute(request);
    assertEquals(Collections.singletonList("write"), invocations());
    assertWorkCleaned();
  }

  @Test
  void outputlessProcessorsWithUntrackedPathsRunEveryTime() throws Exception {
    Map<String, ModernInstallerProfile.DataValue> data =
        Collections.singletonMap(
            "PATCHED", new ModernInstallerProfile.DataValue("[" + GENERATED + "]", null));
    ModernInstallerProfile.Processor first =
        processor("write", Collections.emptyMap(), "{PATCHED}", GOOD);
    ModernInstallerProfile.Processor second =
        processor("require", Collections.emptyMap(), "[" + GENERATED + "]", GOOD);
    ModernInstallerEngine.Request request =
        request(
            root,
            Arrays.asList(first, second),
            data,
            Collections.emptyList(),
            Collections.emptyMap(),
            () -> false);
    execute(request);
    execute(request);
    assertEquals(GOOD, text(maven(root.resolve("libraries"), GENERATED)));
    assertEquals(Arrays.asList("write", "require", "write", "require"), invocations());
    assertWorkCleaned();
  }

  @Test
  void successfulOutputlessRunIsReusedUntilInputOrGeneratedBytesChange() throws Exception {
    Path source = write(maven(root.resolve("libraries"), CACHED_SOURCE), GOOD);
    Path output = write(maven(root.resolve("libraries"), GENERATED), GOOD);
    Files.setLastModifiedTime(output, java.nio.file.attribute.FileTime.fromMillis(1000));
    ModernInstallerEngine.Request request = cachedRequest("cache-copy", () -> false);

    execute(request);
    assertEquals(1, cachedInvocations().size(), "Existing bytes alone must not authorize reuse");
    execute(request);
    assertEquals(1, cachedInvocations().size());

    write(output, "corrupted");
    execute(request);
    assertEquals(GOOD, text(output));
    assertEquals(2, cachedInvocations().size());

    write(source, "updated input");
    execute(request);
    assertEquals("updated input", text(output));
    assertEquals(3, cachedInvocations().size());

    Files.delete(output);
    execute(request);
    assertEquals("updated input", text(output));
    assertEquals(4, cachedInvocations().size());
    execute(request);
    assertEquals(4, cachedInvocations().size());
    assertWorkCleaned();
  }

  @Test
  void failedOutputlessRunCannotKeepAnEarlierSuccessfulReceipt() throws Exception {
    Path source = write(maven(root.resolve("libraries"), CACHED_SOURCE), GOOD);
    Path output = maven(root.resolve("libraries"), GENERATED);
    ModernInstallerEngine.Request request = cachedRequest("cache-copy", () -> false);
    execute(request);
    execute(request);
    assertEquals(1, cachedInvocations().size());

    write(source, "fail");
    assertThrows(IOException.class, () -> execute(request));
    assertEquals(2, cachedInvocations().size());
    write(source, GOOD);
    write(output, GOOD);
    Files.setLastModifiedTime(output, java.nio.file.attribute.FileTime.fromMillis(1000));

    execute(request);
    assertEquals(3, cachedInvocations().size(), "Failure must invalidate the previous receipt");
    execute(request);
    assertEquals(3, cachedInvocations().size());
    assertEquals(GOOD, text(output));
    assertWorkCleaned();
  }

  @Test
  void concurrentEnginesReuseOutputlessResultsOnlyAfterTheWriterCompletes() throws Exception {
    write(maven(root.resolve("libraries"), CACHED_SOURCE), GOOD);
    Path output = maven(root.resolve("libraries"), GENERATED);
    AtomicBoolean cancelled = new AtomicBoolean();
    ModernInstallerEngine.Request request = cachedRequest("cache-wait", cancelled::get);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try {
      Future<Throwable> first = executor.submit(() -> executeFailure(request));
      awaitFile(output.resolveSibling("started"));
      Future<Throwable> second =
          executor.submit(
              () -> {
                secondStarted.countDown();
                return executeFailure(request);
              });
      assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
      write(output.resolveSibling("release"), "release");
      assertNull(first.get(15, TimeUnit.SECONDS));
      assertNull(second.get(15, TimeUnit.SECONDS));
      assertEquals(1, cachedInvocations().size());
      assertEquals(GOOD, text(output));
      assertWorkCleaned();
    } finally {
      cancelled.set(true);
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
    }
  }

  @Test
  void executedProcessorsInvalidateSharedFingerprintsEvenWhenFileMetadataIsPreserved()
      throws Exception {
    write(maven(root.resolve("libraries"), CACHED_SOURCE), GOOD);
    Path vanilla = write(root.resolve("cache/minecraft_1.20.1.jar"), GOOD);
    Path replacement = write(root.resolve("next-vanilla.bin"), GOOD);
    Map<String, ModernInstallerProfile.DataValue> data = new LinkedHashMap<>();
    data.put("SOURCE", new ModernInstallerProfile.DataValue("[" + CACHED_SOURCE + "]", null));
    data.put("GENERATED", new ModernInstallerProfile.DataValue("[" + GENERATED + "]", null));
    ModernInstallerProfile.Processor first =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.singletonList(TOOL),
            Arrays.asList("cache-copy", "{SOURCE}", "[test.engine:first-stage:1]"),
            Collections.singletonList("client"),
            Collections.emptyMap());
    ModernInstallerProfile.Processor middle =
        processor(
            "preserve-time-copy",
            Collections.emptyMap(),
            "{ROOT}/next-vanilla.bin",
            "{MINECRAFT_JAR}");
    ModernInstallerProfile.Processor last =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.singletonList(TOOL),
            Arrays.asList("cache-copy", "{MINECRAFT_JAR}", "{GENERATED}"),
            Collections.singletonList("client"),
            Collections.emptyMap());
    ModernInstallerEngine.Request request =
        request(
            root,
            Arrays.asList(first, middle, last),
            data,
            Collections.emptyList(),
            Collections.emptyMap(),
            () -> false);
    execute(request);
    assertEquals(1, cachedInvocations().size());
    java.nio.file.attribute.FileTime originalTime = Files.getLastModifiedTime(vanilla);

    String changed = "X" + GOOD.substring(1);
    write(replacement, changed);
    execute(request);

    assertEquals(originalTime, Files.getLastModifiedTime(vanilla));
    assertEquals(changed, text(maven(root.resolve("libraries"), GENERATED)));
    assertEquals(2, cachedInvocations().size());
    execute(request);
    assertEquals(2, cachedInvocations().size());
    assertWorkCleaned();
  }

  private ModernInstallerEngine.Request cachedRequest(String mode, BooleanSupplier cancelled)
      throws Exception {
    write(root.resolve("cache/minecraft_1.20.1.jar"), "verified vanilla input");
    Map<String, ModernInstallerProfile.DataValue> data = new LinkedHashMap<>();
    data.put("SOURCE", new ModernInstallerProfile.DataValue("[" + CACHED_SOURCE + "]", null));
    data.put("GENERATED", new ModernInstallerProfile.DataValue("[" + GENERATED + "]", null));
    ModernInstallerProfile.Processor processor =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.singletonList(TOOL),
            Arrays.asList(mode, "{SOURCE}", "{GENERATED}"),
            Collections.singletonList("client"),
            Collections.emptyMap());
    return request(
        root,
        Collections.singletonList(processor),
        data,
        Collections.emptyList(),
        Collections.emptyMap(),
        cancelled);
  }

  private List<String> cachedInvocations() throws IOException {
    return Files.readAllLines(
        maven(root.resolve("libraries"), GENERATED).resolveSibling("cache-invocations.log"),
        StandardCharsets.UTF_8);
  }

  @Test
  void missingEmptyUrlArtifactMustBeDeclaredOutputBeforeAnyChildRuns() throws Exception {
    Library missing = new Library(GENERATED, "", GOOD_HASH, GOOD.length());
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(processor("noop", Collections.emptyMap())),
            Collections.emptyMap(),
            Collections.singletonList(missing),
            Collections.emptyMap(),
            () -> false);
    IOException failure = assertThrows(IOException.class, () -> execute(request));
    assertTrue(failure.getMessage().contains(GENERATED));
    assertTrue(failure.getMessage().contains("attempted sources:"));
    assertFalse(Files.exists(root.resolve("invocations.log")));
    assertWorkCleaned();
  }

  @Test
  void malformedLaterOutputPreflightPreventsEarlierOutputlessChild() throws Exception {
    ModernInstallerEngine.Request request =
        request(
            processor("noop", Collections.emptyMap()),
            processor("noop", outputs("later.bin", "not-a-sha1")));
    assertThrows(IOException.class, () -> execute(request));
    assertFalse(Files.exists(root.resolve("invocations.log")));
    assertWorkCleaned();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void declaredGeneratedArtifactMustMatchItsOwnFinalMetadata(boolean alreadyPresent)
      throws Exception {
    Library generated = new Library(GENERATED, "", GOOD_HASH, GOOD.length());
    if (alreadyPresent) write(maven(root.resolve("libraries"), GENERATED), GOOD);
    String different = "valid intermediate but not the required final artifact";
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor(
                    "write",
                    outputs("[" + GENERATED + "]", DigestUtils.sha1Hex(different)),
                    "[" + GENERATED + "]",
                    different)),
            Collections.emptyMap(),
            Collections.singletonList(generated),
            Collections.emptyMap(),
            () -> false);
    IOException failure = assertThrows(IOException.class, () -> execute(request));
    assertTrue(failure.getMessage().contains(GENERATED));
    assertEquals(Collections.singletonList("write"), invocations());
    // This is a valid declared processor output; final library mismatch must not invent a delete
    // rule.
    assertEquals(different, text(maven(root.resolve("libraries"), GENERATED)));
    assertWorkCleaned();
  }

  @Test
  void exactDeclaredOutputCanSatisfyDeferredGameLibrary() throws Exception {
    Library generated = new Library(GENERATED, "", GOOD_HASH, GOOD.length());
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor(
                    "write",
                    outputs("[" + GENERATED + "]", GOOD_HASH),
                    "[" + GENERATED + "]",
                    GOOD)),
            Collections.emptyMap(),
            Collections.singletonList(generated),
            Collections.emptyMap(),
            () -> false);
    execute(request);
    assertEquals(GOOD, text(maven(root.resolve("libraries"), GENERATED)));
    assertEquals(Collections.singletonList("write"), invocations());
    assertWorkCleaned();
  }

  @Test
  void missingAndMismatchedOutputsFailAndRemoveOnlyInvalidDeclaredFiles() throws Exception {
    Map<String, String> outputs = outputs("missing.bin", GOOD_HASH);
    outputs.put("partial.bin", GOOD_HASH);
    outputs.put("valid.bin", GOOD_HASH);
    Path sentinel = write(root.resolve("unrelated.bin"), "untouched");
    ModernInstallerEngine.Request request =
        request(
            processor("write", outputs, "{ROOT}/partial.bin", "partial", "{ROOT}/valid.bin", GOOD));
    assertThrows(IOException.class, () -> execute(request));
    assertFalse(Files.exists(root.resolve("missing.bin")));
    assertFalse(Files.exists(root.resolve("partial.bin")));
    assertEquals(GOOD, text(root.resolve("valid.bin")));
    assertEquals("untouched", text(sentinel));
    assertWorkCleaned();
  }

  @Test
  void nonzeroChildRetainsValidOutputsAndDeletesMismatchedPartials() throws Exception {
    Map<String, String> outputs = outputs("partial.bin", GOOD_HASH);
    outputs.put("valid.bin", GOOD_HASH);
    ModernInstallerEngine.Request request =
        request(
            processor("exit", outputs, "{ROOT}/partial.bin", "partial", "{ROOT}/valid.bin", GOOD));
    IOException failure = assertThrows(IOException.class, () -> execute(request));
    assertTrue(failure.getSuppressed().length > 0);
    assertFalse(Files.exists(root.resolve("partial.bin")));
    assertEquals(GOOD, text(root.resolve("valid.bin")));
    assertEquals(Collections.singletonList("exit"), invocations());
    assertWorkCleaned();
  }

  @Test
  void restoresExactEmbeddedCacheBeforeLegacyAndRetainsLegacySource() throws Exception {
    String path = MavenCoordinate.parse(GENERATED).getPath();
    Path legacy = write(root.resolve("cache").resolve(path), "wrong legacy bytes");
    Path unrelated = write(root.resolve("cache/unrelated.bin"), "untouched");
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(processor("noop", outputs("[" + GENERATED + "]", GOOD_HASH))),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.singletonMap("cache/" + path, GOOD.getBytes(StandardCharsets.UTF_8)),
            () -> false);
    execute(request);
    assertEquals(GOOD, text(maven(root.resolve("libraries"), GENERATED)));
    assertEquals("wrong legacy bytes", text(legacy));
    assertEquals("untouched", text(unrelated));
    assertFalse(Files.exists(root.resolve("invocations.log")));
    assertWorkCleaned();
  }

  @Test
  void badEmbeddedCacheFallsBackOnlyToExactValidLegacyFile() throws Exception {
    String path = MavenCoordinate.parse(GENERATED).getPath();
    Path legacy = write(root.resolve("cache").resolve(path), GOOD);
    Path unrelated = write(root.resolve("cache/not-the-coordinate.jar"), GOOD);
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(processor("noop", outputs("[" + GENERATED + "]", GOOD_HASH))),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.singletonMap("cache/" + path, "bad cache".getBytes(StandardCharsets.UTF_8)),
            () -> false);
    execute(request);
    Path output = maven(root.resolve("libraries"), GENERATED);
    assertEquals(GOOD, text(output));
    assertFalse(Files.exists(root.resolve("invocations.log")));
    write(output, "published bytes changed later");
    assertEquals(GOOD, text(legacy));
    Files.delete(output);
    Files.delete(legacy);
    assertThrows(IOException.class, () -> execute(request));
    assertEquals(Collections.singletonList("noop"), invocations());
    assertEquals(GOOD, text(unrelated));
    assertWorkCleaned();
  }

  @Test
  void cancellationCleansPartialsTerminatesChildAndReleasesWriterLock() throws Exception {
    AtomicBoolean cancelled = new AtomicBoolean();
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor(
                    "wait",
                    outputs("partial.bin", GOOD_HASH),
                    "{ROOT}/partial.bin",
                    "partial",
                    "{ROOT}/started",
                    "{ROOT}/release")),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            cancelled::get);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Throwable> result = executor.submit(() -> executeFailure(request));
      awaitFile(root.resolve("started"));
      cancelled.set(true);
      assertInstanceOf(InterruptedException.class, result.get(15, TimeUnit.SECONDS));
      assertFalse(Files.exists(root.resolve("partial.bin")));
      assertWorkCleaned();
      try (ModernInstallerLock ignored =
          ModernInstallerLock.acquire(root.resolve("cache"), () -> false)) {
        write(root.resolve("release"), "release");
      }
      // A live fixture would resume and create this after seeing release.
      Thread.sleep(300);
      assertFalse(Files.exists(root.resolve("resumed")));
      execute(request(processor("noop", Collections.emptyMap())));
      assertEquals(Arrays.asList("wait", "noop"), invocations());
    } finally {
      cancelled.set(true);
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
    }
  }

  @Test
  void concurrentEnginesRevalidateAcquiredArtifactsAndSkipCompletedOutputsUnderLock()
      throws Exception {
    AtomicBoolean cancelled = new AtomicBoolean();
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor(
                    "wait",
                    outputs("final.bin", GOOD_HASH),
                    "{ROOT}/final.bin",
                    GOOD,
                    "{ROOT}/started",
                    "{ROOT}/release")),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            cancelled::get);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try {
      Future<Throwable> first = executor.submit(() -> executeFailure(request));
      awaitFile(root.resolve("started"));
      Path acquiredTool = maven(root.resolve("libraries"), TOOL);
      write(acquiredTool, "corrupted while another engine waits");
      Future<Throwable> second =
          executor.submit(
              () -> {
                secondStarted.countDown();
                return executeFailure(request);
              });
      assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
      write(root.resolve("release"), "release");
      assertNull(first.get(15, TimeUnit.SECONDS));
      assertNull(second.get(15, TimeUnit.SECONDS));
      assertEquals(Collections.singletonList("wait"), invocations());
      assertEquals(GOOD, text(root.resolve("final.bin")));
      assertArrayEquals(toolJar, Files.readAllBytes(acquiredTool));
      assertWorkCleaned();
    } finally {
      cancelled.set(true);
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
    }
  }

  @Test
  void configuredRootSymlinkIsAllowedButEscapingOutputChildIsRejected() throws Exception {
    Path alias = symlink(temp.resolve("alias"), root);
    execute(
        request(
            alias,
            Collections.singletonList(
                processor("write", outputs("safe.bin", GOOD_HASH), "{ROOT}/safe.bin", GOOD)),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            () -> false));
    assertEquals(GOOD, text(root.resolve("safe.bin")));
    Path outside = Files.createDirectory(temp.resolve("outside"));
    Path sentinel = write(outside.resolve("victim.bin"), "keep outside");
    symlink(root.resolve("escape"), outside);
    ModernInstallerEngine.Request request =
        request(
            processor("noop", Collections.emptyMap()),
            processor(
                "write",
                outputs("escape/victim.bin", GOOD_HASH),
                "{ROOT}/escape/victim.bin",
                GOOD));
    assertThrows(IOException.class, () -> execute(request));
    assertEquals("keep outside", text(sentinel));
    assertEquals(Collections.singletonList("write"), invocations());
    assertTrue(Files.isSymbolicLink(root.resolve("escape")));
    assertWorkCleaned();
  }

  @Test
  void zeroClientProfileDoesNotCreateCacheOrAcquireLock() throws Exception {
    ModernInstallerProfile.Processor server =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("server"),
            Collections.emptyMap());
    execute(
        request(
            root,
            Collections.singletonList(server),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyMap(),
            () -> true));
    assertFalse(Files.exists(root.resolve("cache")));
    assertFalse(Files.exists(root.resolve("libraries")));
    assertFalse(Files.exists(root.resolve("invocations.log")));
  }

  @Test
  void classpathChangedByAnEarlierProcessorIsRejectedBeforeTheNextChild() throws Exception {
    String dependency = "test.engine:dependency:1";
    ModernInstallerProfile.Processor first =
        processor("write", Collections.emptyMap(), "[" + dependency + "]", "corrupted dependency");
    ModernInstallerProfile.Processor second =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.singletonList(dependency),
            Arrays.asList("noop", "{ROOT}/invocations.log"),
            Collections.singletonList("client"),
            Collections.emptyMap());
    Library library = new Library(dependency, "", DigestUtils.sha1Hex(toolJar), toolJar.length);
    ModernInstallerEngine.Request request =
        request(
            root,
            Arrays.asList(first, second),
            Collections.emptyMap(),
            Collections.singletonList(library),
            Collections.singletonMap(
                "maven/" + MavenCoordinate.parse(dependency).getPath(), toolJar),
            () -> false);
    IOException failure = assertThrows(IOException.class, () -> execute(request));
    assertTrue(failure.getMessage().contains(dependency));
    assertEquals(Collections.singletonList("write"), invocations());
    assertEquals("corrupted dependency", text(maven(root.resolve("libraries"), dependency)));
    assertWorkCleaned();
  }

  @Test
  void extractedClientResourceLivesOnlyForTheProcessorSequence() throws Exception {
    Map<String, ModernInstallerProfile.DataValue> data =
        Collections.singletonMap(
            "RESOURCE", new ModernInstallerProfile.DataValue("/data/input.txt", null));
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor(
                    "copy", outputs("copied.bin", GOOD_HASH), "{RESOURCE}", "{ROOT}/copied.bin")),
            data,
            Collections.emptyList(),
            Collections.singletonMap("data/input.txt", GOOD.getBytes(StandardCharsets.UTF_8)),
            () -> false);
    execute(request);
    assertEquals(GOOD, text(root.resolve("copied.bin")));
    assertEquals(Collections.singletonList("copy"), invocations());
    assertWorkCleaned();
  }

  @Test
  void canonicalProcessorPathCanShareVerifiedBytesWithGameArtifactAlias() throws Exception {
    Library alias =
        MojangUtils.getGson()
            .fromJson(
                "{\"name\":\"test.engine:alias:1\",\"downloads\":{\"artifact\":{\"path\":\""
                    + MavenCoordinate.parse(TOOL).getPath()
                    + "\",\"sha1\":\""
                    + DigestUtils.sha1Hex(toolJar)
                    + "\",\"size\":"
                    + toolJar.length
                    + ",\"url\":\"\"}}}",
                Library.class);
    ModernInstallerEngine.Request request =
        request(
            root,
            Collections.singletonList(
                processor("write", outputs("result.bin", GOOD_HASH), "{ROOT}/result.bin", GOOD)),
            Collections.emptyMap(),
            Collections.singletonList(alias),
            Collections.emptyMap(),
            () -> false);

    execute(request);

    assertEquals(GOOD, text(root.resolve("result.bin")));
    assertEquals(Collections.singletonList("write"), invocations());
    assertWorkCleaned();
  }

  private ModernInstallerEngine.Request request(ModernInstallerProfile.Processor... processors)
      throws Exception {
    return request(
        root,
        Arrays.asList(processors),
        Collections.emptyMap(),
        Collections.emptyList(),
        Collections.emptyMap(),
        () -> false);
  }

  private ModernInstallerEngine.Request request(
      Path launcherRoot,
      List<ModernInstallerProfile.Processor> processors,
      Map<String, ModernInstallerProfile.DataValue> data,
      List<Library> gameLibraries,
      Map<String, byte[]> embedded,
      BooleanSupplier cancelled)
      throws Exception {
    Library tool = new Library(TOOL, "", DigestUtils.sha1Hex(toolJar), toolJar.length);
    ModernInstallerProfile profile =
        new ModernInstallerProfile(
            1,
            "engine test",
            "test-loader",
            "1.20.1",
            "version.json",
            "test-loader",
            "1.20.1",
            new byte[0],
            Collections.singletonList(MojangUtils.getGson().toJson(tool)),
            processors,
            data);
    Path installer = Files.createTempFile(temp, "installer-", ".jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(installer))) {
      entry(zip, "maven/" + MavenCoordinate.parse(TOOL).getPath(), toolJar);
      for (Map.Entry<String, byte[]> entry : embedded.entrySet()) {
        entry(zip, entry.getKey(), entry.getValue());
      }
    }
    IJavaRuntime runtime = runtime();
    return new ModernInstallerEngine.Request(
        launcherRoot,
        installer,
        launcherRoot.resolve("cache/minecraft_1.20.1.jar"),
        profile,
        ModernInstallerArtifactResolver.plan(profile, gameLibraries, runtime),
        runtime,
        cancelled);
  }

  private static ModernInstallerProfile.Processor processor(
      String mode, Map<String, String> outputs, String... arguments) {
    List<String> args = new ArrayList<>();
    args.add(mode);
    args.add("{ROOT}/invocations.log");
    args.addAll(Arrays.asList(arguments));
    return new ModernInstallerProfile.Processor(
        TOOL, Arrays.asList(TOOL, TOOL), args, Collections.singletonList("client"), outputs);
  }

  private static Map<String, String> outputs(String target, String hash) {
    Map<String, String> outputs = new LinkedHashMap<>();
    outputs.put(target, hash);
    return outputs;
  }

  private static void execute(ModernInstallerEngine.Request request) throws Exception {
    new ModernInstallerEngine().execute(request, REPORTER);
  }

  private static Throwable executeFailure(ModernInstallerEngine.Request request) {
    try {
      execute(request);
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(bytes);
    zip.closeEntry();
  }

  private static Path maven(Path repository, String coordinate) {
    return MavenCoordinate.resolve(repository, MavenCoordinate.parse(coordinate).getPath());
  }

  private static Path write(Path path, String value) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.write(path, value.getBytes(StandardCharsets.UTF_8));
  }

  private static String text(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  private List<String> invocations() throws IOException {
    return Files.readAllLines(root.resolve("invocations.log"), StandardCharsets.UTF_8);
  }

  private void assertWorkCleaned() throws IOException {
    try (Stream<Path> children = Files.list(root.resolve("cache"))) {
      assertFalse(
          children.anyMatch(
              path ->
                  Files.isDirectory(path)
                      && path.getFileName().toString().startsWith("modern-installer-")));
    }
  }

  private static void awaitFile(Path path) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (!Files.isRegularFile(path)) {
      if (System.nanoTime() >= deadline) fail("Child did not reach its barrier: " + path);
      Thread.sleep(20);
    }
  }

  private static Path symlink(Path link, Path target) throws IOException {
    try {
      return Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | FileSystemException unavailable) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "Symbolic links unavailable: " + unavailable);
      throw new AssertionError("unreachable", unavailable);
    }
  }

  private static IJavaRuntime runtime() {
    return new IJavaRuntime() {
      public File getExecutableFile() {
        String executable =
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return new File(System.getProperty("java.home"), "bin/" + executable);
      }

      public String getVersion() {
        return System.getProperty("java.version");
      }

      public String getVendor() {
        return System.getProperty("java.vendor");
      }

      public String getOsArch() {
        return System.getProperty("os.arch");
      }

      public String getBitness() {
        return "64";
      }

      public boolean is64Bit() {
        return true;
      }

      public boolean isValid() {
        return true;
      }
    };
  }

  /** Packaged alone into a processor JAR; it cannot use JUnit or launcher classes. */
  public static final class ChildProcessor {
    public static void main(String[] args) throws Exception {
      String mode = args[0];
      if (mode.startsWith("cache-")) {
        Path source = Paths.get(args[1]);
        Path output = Paths.get(args[2]);
        Files.createDirectories(output.getParent());
        Files.write(
            output.resolveSibling("cache-invocations.log"),
            (mode + "\n").getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
        byte[] bytes = Files.readAllBytes(source);
        Files.write(output, bytes);
        if (mode.equals("cache-wait")) {
          Files.write(output.resolveSibling("started"), new byte[] {1});
          while (!Files.exists(output.resolveSibling("release"))) Thread.sleep(20);
        }
        if (new String(bytes, StandardCharsets.UTF_8).equals("fail")) {
          throw new IOException("Processor failed after writing its output");
        }
        return;
      }
      Path log = Paths.get(args[1]);
      Files.write(
          log,
          (mode + "\n").getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
      if (mode.equals("noop")) return;
      if (mode.equals("require")) {
        String actual = new String(Files.readAllBytes(Paths.get(args[2])), StandardCharsets.UTF_8);
        if (!actual.equals(args[3])) throw new IOException("Generated input has wrong content");
        return;
      }
      if (mode.equals("preserve-time-copy")) {
        Path output = Paths.get(args[3]);
        java.nio.file.attribute.FileTime time = Files.getLastModifiedTime(output);
        Files.write(output, Files.readAllBytes(Paths.get(args[2])));
        Files.setLastModifiedTime(output, time);
        return;
      }
      if (mode.equals("copy")) {
        Path output = Paths.get(args[3]);
        Files.createDirectories(output.getParent());
        Files.copy(Paths.get(args[2]), output);
        return;
      }
      if (mode.equals("wait")) {
        Path output = Paths.get(args[2]);
        Files.createDirectories(output.getParent());
        Files.write(output, args[3].getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(args[4]), new byte[] {1});
        while (!Files.exists(Paths.get(args[5]))) Thread.sleep(20);
        Files.write(log.resolveSibling("resumed"), new byte[] {1});
        return;
      }
      for (int index = 2; index < args.length; index += 2) {
        Path output = Paths.get(args[index]);
        Files.createDirectories(output.getParent());
        Files.write(output, args[index + 1].getBytes(StandardCharsets.UTF_8));
      }
      if (mode.equals("exit")) System.exit(7);
    }
  }
}
