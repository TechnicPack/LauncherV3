package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProcessorCacheTest {
  private static final String TOOL = "test.cache:processor:1";
  private static final String INPUT = "test.cache:mcp_config:1@zip";
  private static final String OUTPUT = "test.cache:generated:1";
  private static final String GOOD = "complete output";
  private static final BooleanSupplier RUNNING = () -> false;

  @TempDir Path temp;
  private Path root;
  private Path installer;
  private Path vanilla;
  private Path executable;
  private Path tool;
  private Path input;
  private Path output;
  private Path work;
  private ModernInstallerLock lock;
  private ModernInstallerProfile profile;

  @BeforeEach
  void createInputsAndAcquireInstallerLock() throws Exception {
    root = Files.createDirectory(temp.resolve("root"));
    installer = temp.resolve("installer.jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(installer))) {
      zip.putNextEntry(new ZipEntry("data/patch.bin"));
      zip.write(bytes("embedded patch"));
      zip.closeEntry();
    }
    vanilla = write(root.resolve("cache/vanilla.jar"), "vanilla client");
    executable = write(temp.resolve("runtime/bin/java"), "test runtime executable");
    tool = write(maven(TOOL), "processor bytecode");
    input = write(maven(INPUT), "mcp input data");
    output = maven(OUTPUT);
    work = Files.createDirectories(root.resolve("cache/work-one"));
    profile = profile(defaultArguments(), data());
    lock = ModernInstallerLock.acquire(root.resolve("cache"), RUNNING);
  }

  @AfterEach
  void releaseInstallerLock() throws Exception {
    if (lock != null) lock.close();
  }

  @Test
  void existingFilesAreNotAdoptedAndOnlyAnObservedSuccessfulWritePersists() throws Exception {
    write(output, "preexisting output");
    ProcessorCache.Entry untouched = prepare();
    assertFalse(untouched.isValid());
    untouched.record();
    assertFalse(prepare().isValid(), "Existing bytes alone are not successful-run evidence");

    ProcessorCache.Entry run = prepare();
    run.invalidate();
    write(output, GOOD);
    assertFalse(prepare().isValid(), "A write without a successful record is not reusable");
    run.record();
    assertTrue(prepare().isValid(), "A fresh cache instance must reuse the persisted success");
  }

  @Test
  void sameContentRewriteEstablishesProvenanceButPureInputNoOpDoesNot() throws Exception {
    write(output, GOOD);
    Files.setLastModifiedTime(output, FileTime.fromMillis(1_600_000_000_000L));
    ProcessorCache.Entry rewrite = prepare();
    rewrite.invalidate();
    write(output, GOOD);
    Files.setLastModifiedTime(output, FileTime.fromMillis(1_600_000_060_000L));
    rewrite.record();
    assertTrue(prepare().isValid());

    ModernInstallerProfile pureInput =
        profile(Arrays.asList("--input", "[" + INPUT + "]"), Collections.emptyMap());
    ProcessorCache.Entry noOp = prepare(pureInput, 0, runtime(), work);
    assertFalse(noOp.isValid());
    noOp.invalidate();
    noOp.record();
    assertFalse(prepare(pureInput, 0, runtime(), work).isValid());
  }

  @Test
  void changedCorruptAndDeletedGeneratedBytesAllMissDespiteMatchingMetadata() throws Exception {
    recordSuccess();
    write(output, "changed output with different size");
    assertFalse(prepare().isValid());

    recordSuccess();
    FileTime successfulTime = Files.getLastModifiedTime(output);
    write(output, "corrupt! output");
    assertEquals(bytes(GOOD).length, Files.size(output));
    Files.setLastModifiedTime(output, successfulTime);
    assertFalse(
        prepare().isValid(), "Matching size and timestamp cannot replace byte verification");

    recordSuccess();
    Files.delete(output);
    assertFalse(prepare().isValid());
  }

  @Test
  void directBracketInputsAreHashedAndCannotBeDeletedDuringInvalidation() throws Exception {
    recordSuccess();
    FileTime inputTime = Files.getLastModifiedTime(input);
    write(input, "bad input data");
    Files.setLastModifiedTime(input, inputTime);
    ProcessorCache.Entry changedInput = prepare();
    assertFalse(changedInput.isValid());
    changedInput.invalidate();
    assertArrayEquals(bytes("bad input data"), Files.readAllBytes(input));
    assertArrayEquals(bytes(GOOD), Files.readAllBytes(output));
  }

  @Test
  void invalidationBeforeAFailedRunRevokesTheOldSuccessWithoutDeletingReferences()
      throws Exception {
    recordSuccess();
    ProcessorCache.Entry run = prepare();
    assertTrue(run.isValid());
    run.invalidate();
    assertArrayEquals(bytes(GOOD), Files.readAllBytes(output));
    assertArrayEquals(bytes("mcp input data"), Files.readAllBytes(input));
    assertFalse(prepare().isValid());
    write(output, "partial failed output");
    assertFalse(prepare().isValid(), "Failure never calls record even if files exist");
  }

  @Test
  void recipeScalarArgumentsAndProcessorPositionArePartOfTheIdentity() throws Exception {
    recordSuccess();
    List<String> changedArguments = new ArrayList<>(defaultArguments());
    changedArguments.set(1, "transform");
    ModernInstallerProfile changedRecipe = profile(changedArguments, data());
    assertFalse(prepare(changedRecipe, 0, runtime(), work).isValid());
    assertFalse(prepare(profile, 1, runtime(), work).isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"installer", "vanilla", "classpath"})
  void fixedInputContentChangesInvalidatePriorSuccess(String changedInput) throws Exception {
    recordSuccess();
    corruptPreservingMetadata(fixedInput(changedInput));
    assertFalse(prepare().isValid());
  }

  @Test
  void classpathLocationMattersEvenWhenBytesMatch() throws Exception {
    recordSuccess();
    Path alternative = write(maven("test.cache:other-processor:1"), "processor bytecode");
    ProcessorCache.Entry entry =
        prepare(
            cache(runtime(), RUNNING), profile, 0, work, Collections.singletonList(alternative));
    assertNotNull(entry);
    assertFalse(entry.isValid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"path", "bytes", "version"})
  void selectedRuntimeIdentityInvalidatesPriorSuccess(String changedIdentity) throws Exception {
    recordSuccess();
    Path selected = executable;
    if (changedIdentity.equals("path")) {
      selected = write(temp.resolve("other-runtime/bin/java"), "test runtime executable");
    } else if (changedIdentity.equals("bytes")) {
      corruptPreservingMetadata(executable);
    }
    IJavaRuntime changed =
        runtime(
            selected,
            changedIdentity.equals("version") ? "21.0.2" : "21.0.1",
            "test vendor",
            "amd64",
            "64");
    assertFalse(prepare(profile, 0, changed, work).isValid());
  }

  @Test
  @org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
  void selectedJavawRuntimeFingerprintsTheExecutedConsoleBinary() throws Exception {
    Path console = write(executable.resolveSibling("java.exe"), "console executable");
    Path windowed = write(executable.resolveSibling("javaw.exe"), "windowed executable");
    IJavaRuntime selected = runtime(windowed, "21.0.1", "test vendor", "amd64", "64");
    ProcessorCache.Entry run = prepare(profile, 0, selected, work);
    run.invalidate();
    write(output, GOOD);
    run.record();
    assertTrue(prepare(profile, 0, selected, work).isValid());

    corruptPreservingMetadata(console);
    assertFalse(prepare(profile, 0, selected, work).isValid());
  }

  @Test
  void embeddedInputsUseStableTokenIdentityInsteadOfTemporaryExtractionPaths() throws Exception {
    ModernInstallerProfile embedded = embeddedProfile();
    ProcessorCache.Entry run = prepare(embedded, 0, runtime(), work);
    assertFalse(run.isValid());
    run.invalidate();
    write(output, GOOD);
    run.record();
    Files.delete(work.resolve("data/patch.bin"));
    Path otherWork = Files.createDirectories(root.resolve("cache/work-two"));
    assertTrue(prepare(embedded, 0, runtime(), otherWork).isValid());
  }

  @Test
  void reservedInputTokensTakePrecedenceOverProfileData() throws Exception {
    Map<String, ModernInstallerProfile.DataValue> reserved = data();
    reserved.put(
        "MINECRAFT_JAR", new ModernInstallerProfile.DataValue("[test.cache:shadow:1]", null));
    List<String> arguments = new ArrayList<>(defaultArguments());
    arguments.add("{MINECRAFT_JAR}");
    arguments.add("{INSTALLER}");
    ModernInstallerProfile withReserved = profile(arguments, reserved);
    ProcessorCache.Entry run = prepare(withReserved, 0, runtime(), work);
    assertFalse(run.isValid());
    run.invalidate();
    write(output, GOOD);
    run.record();
    assertTrue(prepare(withReserved, 0, runtime(), work).isValid());
    assertFalse(Files.exists(maven("test.cache:shadow:1")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"vanilla", "classpath", "embedded"})
  void fixedInputsChangedDuringExecutionCannotPublishASuccess(String changedInput)
      throws Exception {
    ModernInstallerProfile selected = changedInput.equals("embedded") ? embeddedProfile() : profile;
    ProcessorCache cache = cache(runtime(), RUNNING);
    ProcessorCache.Entry run = prepare(cache, selected, 0, work, Collections.singletonList(tool));
    assertNotNull(run);
    run.invalidate();
    Path fixed = fixedInput(changedInput);
    byte[] original = Files.readAllBytes(fixed);
    write(output, GOOD);
    corruptPreservingMetadata(fixed);
    run.record();
    Files.write(fixed, original);
    assertFalse(prepare(selected, 0, runtime(), work).isValid());
  }

  @Test
  void everyTrackedReferenceMustExistAtSuccessfulCompletion() throws Exception {
    ProcessorCache.Entry run = prepare();
    run.invalidate();
    write(output, GOOD);
    Files.delete(input);
    run.record();
    write(input, "mcp input data");
    assertFalse(prepare().isValid(), "Restoring an input must not revive an incomplete success");
  }

  @Test
  void malformedTruncatedAndTrailingStateFailClosedWithoutTouchingArtifacts() throws Exception {
    recordSuccess();
    Map<Path, byte[]> receipts = new LinkedHashMap<>();
    for (Path file : stateFiles()) receipts.put(file, Files.readAllBytes(file));
    assertFalse(receipts.isEmpty());

    for (int damage = 0; damage < 3; damage++) {
      for (Map.Entry<Path, byte[]> receipt : receipts.entrySet()) {
        byte[] original = receipt.getValue();
        byte[] damaged;
        if (damage == 0) {
          damaged = bytes("not a successful processor receipt");
        } else if (damage == 1) {
          damaged = Arrays.copyOf(original, original.length / 2);
        } else {
          damaged = Arrays.copyOf(original, original.length + 1);
          damaged[original.length] = 1;
        }
        Files.write(receipt.getKey(), damaged);
      }
      assertFalse(prepare().isValid());
      assertArrayEquals(bytes(GOOD), Files.readAllBytes(output));
      assertArrayEquals(bytes("mcp input data"), Files.readAllBytes(input));
      for (Map.Entry<Path, byte[]> receipt : receipts.entrySet()) {
        Files.write(receipt.getKey(), receipt.getValue());
      }
      assertTrue(prepare().isValid());
    }
  }

  @Test
  void aForeignReceiptCannotAuthorizeAnotherProcessorRecipe() throws Exception {
    recordSuccess();
    byte[] originalReceipt = Files.readAllBytes(stateFiles().get(0));
    List<String> changedArguments = new ArrayList<>(defaultArguments());
    changedArguments.set(1, "transform");
    ModernInstallerProfile changed = profile(changedArguments, data());
    write(output, "before transformation");
    ProcessorCache.Entry run = prepare(changed, 0, runtime(), work);
    run.invalidate();
    write(output, GOOD);
    run.record();
    assertTrue(prepare(changed, 0, runtime(), work).isValid());

    for (Path file : stateFiles()) Files.write(file, originalReceipt);
    assertFalse(prepare(changed, 0, runtime(), work).isValid());
    assertArrayEquals(bytes(GOOD), Files.readAllBytes(output));
  }

  @Test
  void cancellationAfterProcessorWriteCannotPublishSuccess() throws Exception {
    AtomicBoolean cancelled = new AtomicBoolean();
    ProcessorCache.Entry run =
        prepare(
            cache(runtime(), cancelled::get), profile, 0, work, Collections.singletonList(tool));
    assertNotNull(run);
    run.invalidate();
    write(output, GOOD);
    cancelled.set(true);
    assertThrows(InterruptedException.class, run::record);
    cancelled.set(false);
    assertFalse(prepare().isValid());
    assertTrue(stateFiles().isEmpty(), "Cancellation must not leave a receipt or owned stage");
    assertArrayEquals(bytes(GOOD), Files.readAllBytes(output));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{ROOT}",
        "{LIBRARY_DIR}",
        "{ROOT}/untracked.bin",
        "--output={OUTPUT}",
        "'{OUTPUT}'",
        "\\{OUTPUT\\}",
        "untracked.jar",
        "relative/untracked",
        "C:\\untracked.jar"
      })
  void unsupportedPathOrTokenShapesDisableReuse(String unsupported) throws Exception {
    List<String> arguments = new ArrayList<>(defaultArguments());
    arguments.add(unsupported);
    ModernInstallerProfile untrackable = profile(arguments, data());
    assertNull(
        prepare(cache(runtime(), RUNNING), untrackable, 0, work, Collections.singletonList(tool)));
    assertFalse(Files.exists(output));
  }

  @Test
  void scalarsWithoutAMavenReferenceCannotEstablishACache() throws Exception {
    ModernInstallerProfile scalars =
        profile(Arrays.asList("--task", "copy", "{SIDE}"), Collections.emptyMap());
    assertNull(
        prepare(cache(runtime(), RUNNING), scalars, 0, work, Collections.singletonList(tool)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"installer", "vanilla", "executable", "classpath"})
  void absentFixedInputsFallBackToOrdinaryExecution(String missingInput) throws Exception {
    Files.delete(fixedInput(missingInput));
    assertNull(
        prepare(cache(runtime(), RUNNING), profile, 0, work, Collections.singletonList(tool)));
    assertFalse(Files.exists(output));
  }

  @Test
  void aReferenceReplacedByAnEscapingSymlinkCannotReuseOrDeleteExternalBytes() throws Exception {
    recordSuccess();
    Path external = write(temp.resolve("outside/generated.jar"), GOOD);
    Files.delete(output);
    symlink(output, external);
    try {
      ProcessorCache.Entry entry =
          prepare(cache(runtime(), RUNNING), profile, 0, work, Collections.singletonList(tool));
      if (entry != null) {
        assertFalse(entry.isValid());
        entry.invalidate();
        entry.record();
      }
    } catch (IOException rejected) {
      // The path guard may reject the reference before an entry can be constructed.
    }
    assertTrue(Files.isSymbolicLink(output));
    assertArrayEquals(bytes(GOOD), Files.readAllBytes(external));
  }

  @Test
  void escapingStateDirectoryCannotAdoptOrOverwriteAnExternalReceipt() throws Exception {
    recordSuccess();
    Path state = root.resolve("cache/processor-state");
    Path external = temp.resolve("outside-state");
    Files.move(state, external);
    Map<Path, byte[]> original = new LinkedHashMap<>();
    try (Stream<Path> paths = Files.walk(external)) {
      for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
        original.put(path, Files.readAllBytes(path));
      }
    }
    symlink(state, external);
    try {
      ProcessorCache.Entry entry =
          prepare(cache(runtime(), RUNNING), profile, 0, work, Collections.singletonList(tool));
      if (entry != null) {
        assertFalse(entry.isValid());
        entry.invalidate();
        write(output, "new processor result");
        entry.record();
      }
    } catch (IOException rejected) {
      // Unsafe metadata paths may be rejected rather than treated as an ordinary miss.
    }
    for (Map.Entry<Path, byte[]> receipt : original.entrySet()) {
      assertArrayEquals(receipt.getValue(), Files.readAllBytes(receipt.getKey()));
    }
    try (Stream<Path> paths = Files.walk(external)) {
      assertEquals(
          original.keySet(), paths.filter(Files::isRegularFile).collect(Collectors.toSet()));
    }
    assertTrue(Files.isSymbolicLink(state));
  }

  private void recordSuccess() throws Exception {
    ProcessorCache.Entry run = prepare();
    run.invalidate();
    write(output, GOOD);
    run.record();
    assertTrue(prepare().isValid());
  }

  private ProcessorCache.Entry prepare() throws Exception {
    return prepare(profile, 0, runtime(), work);
  }

  private ProcessorCache.Entry prepare(
      ModernInstallerProfile selected, int index, IJavaRuntime runtime, Path workDirectory)
      throws Exception {
    ProcessorCache.Entry entry =
        prepare(
            cache(runtime, RUNNING),
            selected,
            index,
            workDirectory,
            Collections.singletonList(tool));
    assertNotNull(entry, "The fixture has conservatively trackable Maven arguments");
    return entry;
  }

  private ProcessorCache.Entry prepare(
      ProcessorCache cache,
      ModernInstallerProfile selected,
      int index,
      Path workDirectory,
      List<Path> classpath)
      throws Exception {
    Map<String, String> resolved =
        InstallerTokenResolver.resolveData(selected, root, installer, vanilla, workDirectory);
    ModernInstallerProfile.Processor processor = selected.getProcessors().get(0);
    List<String> arguments = new ArrayList<>();
    for (String argument : processor.getArgs()) {
      arguments.add(
          InstallerTokenResolver.resolveArgument(argument, resolved, root.resolve("libraries")));
    }
    return cache.prepare(selected, processor, index, resolved, arguments, classpath);
  }

  private ProcessorCache cache(IJavaRuntime runtime, BooleanSupplier cancelled) {
    return new ProcessorCache(root, installer, vanilla, runtime, cancelled);
  }

  private static List<String> defaultArguments() {
    return Arrays.asList("--task", "copy", "--input", "[" + INPUT + "]", "--output", "{OUTPUT}");
  }

  private static Map<String, ModernInstallerProfile.DataValue> data() {
    Map<String, ModernInstallerProfile.DataValue> data = new LinkedHashMap<>();
    data.put("OUTPUT", new ModernInstallerProfile.DataValue("[" + OUTPUT + "]", null));
    return data;
  }

  private ModernInstallerProfile embeddedProfile() {
    Map<String, ModernInstallerProfile.DataValue> embedded = data();
    embedded.put("PATCH", new ModernInstallerProfile.DataValue("/data/patch.bin", null));
    List<String> arguments = new ArrayList<>(defaultArguments());
    arguments.add("{PATCH}");
    return profile(arguments, embedded);
  }

  private static ModernInstallerProfile profile(
      List<String> arguments, Map<String, ModernInstallerProfile.DataValue> data) {
    ModernInstallerProfile.Processor processor =
        new ModernInstallerProfile.Processor(
            TOOL,
            Collections.emptyList(),
            arguments,
            Collections.singletonList("client"),
            Collections.emptyMap());
    return new ModernInstallerProfile(
        1,
        "cache test",
        "test-loader",
        "1.20.1",
        "version.json",
        "test-loader",
        "1.20.1",
        bytes("{}"),
        Collections.emptyList(),
        Collections.singletonList(processor),
        data);
  }

  private Path fixedInput(String name) {
    switch (name) {
      case "installer":
        return installer;
      case "vanilla":
        return vanilla;
      case "executable":
        return executable;
      case "classpath":
        return tool;
      case "embedded":
        return work.resolve("data/patch.bin");
      default:
        throw new IllegalArgumentException(name);
    }
  }

  private List<Path> stateFiles() throws IOException {
    Path state = root.resolve("cache/processor-state");
    if (!Files.exists(state, LinkOption.NOFOLLOW_LINKS)) return Collections.emptyList();
    try (Stream<Path> paths = Files.walk(state)) {
      return paths
          .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
          .collect(Collectors.toList());
    }
  }

  private Path maven(String coordinate) {
    return MavenCoordinate.resolve(
        root.resolve("libraries"), MavenCoordinate.parse(coordinate).getPath());
  }

  private IJavaRuntime runtime() {
    return runtime(executable, "21.0.1", "test vendor", "amd64", "64");
  }

  private static IJavaRuntime runtime(
      Path executable, String version, String vendor, String architecture, String bitness) {
    return new IJavaRuntime() {
      public File getExecutableFile() {
        return executable.toFile();
      }

      public String getVersion() {
        return version;
      }

      public String getVendor() {
        return vendor;
      }

      public String getOsArch() {
        return architecture;
      }

      public String getBitness() {
        return bitness;
      }

      public boolean is64Bit() {
        return bitness.equals("64");
      }

      public boolean isValid() {
        return true;
      }
    };
  }

  private static void corruptPreservingMetadata(Path path) throws IOException {
    FileTime modified = Files.getLastModifiedTime(path);
    byte[] content = Files.readAllBytes(path);
    content[content.length / 2] ^= 1;
    Files.write(path, content);
    Files.setLastModifiedTime(path, modified);
  }

  private static Path write(Path path, String content) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.write(path, bytes(content));
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static Path symlink(Path link, Path target) throws IOException {
    try {
      return Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | FileSystemException unavailable) {
      assumeTrue(false, "Symbolic links unavailable: " + unavailable);
      throw new AssertionError("unreachable", unavailable);
    }
  }
}
