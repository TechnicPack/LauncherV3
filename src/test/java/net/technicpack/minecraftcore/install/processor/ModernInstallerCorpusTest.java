package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.install.processor.ModernInstallerArtifactResolver.ArtifactPlan;
import net.technicpack.minecraftcore.install.processor.ModernInstallerArtifactResolver.ArtifactRequest;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Opt-in metadata-only coverage; never downloads artifacts, extracts resources or runs processors.
 */
class ModernInstallerCorpusTest {
  private static final String FORGE_MIRROR = "FORGE_INSTALLER_MIRROR";
  private static final String NEOFORGE_MIRROR = "NEOFORGE_INSTALLER_MIRROR";
  private static final String EMPTY_DATA_FIXTURE = "forge-1.12.2-14.23.5.2851-installer.jar";
  private static final Set<String> MISMATCH_FIXTURES =
      new LinkedHashSet<>(
          Arrays.asList(
              "neoforge-20.6.15-beta-installer.jar", "neoforge-20.6.16-beta-installer.jar"));
  private static final Set<String> INFERRED_TOOL_FIXTURES =
      new LinkedHashSet<>(
          Arrays.asList(
              "neoforge-20.6.84-beta-installer.jar",
              "neoforge-20.6.85-beta-installer.jar",
              "neoforge-20.6.87-beta-installer.jar"));
  private static final List<String> EXPECTED_INFERRED_TOOLS =
      Arrays.asList(
          "com.github.jponge:lzma-java:1.3",
          "com.google.code.gson:gson:2.10.1",
          "com.nothome:javaxdelta:2.0.1",
          "net.neoforged.installertools:binarypatcher:2.1.2",
          "net.neoforged.installertools:cli-utils:2.1.4",
          "net.neoforged.installertools:jarsplitter:2.1.2",
          "net.neoforged.javadoctor:gson-io:2.0.17",
          "net.neoforged.javadoctor:spec:2.0.17",
          "net.neoforged:AutoRenamingTool:2.0.3:all",
          "net.sf.jopt-simple:jopt-simple:6.0-alpha-3",
          "trove:trove:1.0.2");

  @Test
  void parsesEveryInstaller() throws IOException {
    assumeTrue(
        System.getenv(FORGE_MIRROR) != null || System.getenv(NEOFORGE_MIRROR) != null,
        "Set FORGE_INSTALLER_MIRROR and/or NEOFORGE_INSTALLER_MIRROR to scan local installers");
    assertAll(
        "Supplied installer mirrors",
        Stream.of(FORGE_MIRROR, NEOFORGE_MIRROR)
            .filter(variable -> System.getenv(variable) != null)
            .map(variable -> (Executable) () -> scanMirror(variable, System.getenv(variable))));
  }

  private static void scanMirror(String variable, String value) throws IOException {
    assertFalse(value.trim().isEmpty(), variable + " was supplied but is empty");
    Path root = Paths.get(value).toAbsolutePath().normalize();
    assertTrue(Files.isDirectory(root), variable + " is not an existing directory: " + root);
    assertTrue(Files.isReadable(root), variable + " is unreadable: " + root);
    List<Path> installers = collectInstallers(root);
    CorpusCounts counts = new CorpusCounts(installers.size());
    IJavaRuntime runtime = runtime();
    try {
      assertAll(
          variable + " at " + root,
          Stream.concat(
              installers.stream()
                  .map(
                      installer ->
                          (Executable)
                              () -> {
                                try {
                                  validateInstaller(installer, runtime, counts);
                                } catch (Exception | AssertionError failure) {
                                  throw new AssertionError(
                                      "Installer corpus failure: " + installer, failure);
                                }
                              }),
              Stream.of((Executable) () -> counts.requireCoverage(variable, root))));
    } finally {
      System.out.println(variable + " at " + root + ": " + counts);
    }
  }

  private static List<Path> collectInstallers(Path root) throws IOException {
    List<Path> installers = new ArrayList<>();
    Files.walkFileTree(
        root,
        EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
              throws IOException {
            if (!Files.isReadable(directory)) {
              throw new IOException("Unreadable mirror directory: " + directory);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (file.getFileName().toString().endsWith("-installer.jar")) {
              if (!attributes.isRegularFile() || !Files.isReadable(file)) {
                throw new IOException("Installer is not a readable regular file: " + file);
              }
              installers.add(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException failure)
              throws IOException {
            throw new IOException("Cannot inspect mirror entry: " + file, failure);
          }
        });
    Collections.sort(installers);
    return installers;
  }

  private static void validateInstaller(Path installer, IJavaRuntime runtime, CorpusCounts counts)
      throws IOException, InterruptedException {
    ModernInstallerProfile profile = ModernInstallerProfileReader.read(installer);
    JsonObject raw = readRawProfile(installer);
    if (profile == null) {
      if (raw == null) {
        counts.withoutProfile++;
      } else {
        assertFalse(raw.has("spec"), "A spec-bearing installer must not be classified as legacy");
        assertTrue(raw.has("install") && raw.has("versionInfo"), "Unknown legacy profile shape");
        counts.legacy++;
      }
      return;
    }
    counts.modern++;
    assertNotNull(raw, "Modern profile must retain its source install_profile.json");
    JsonPrimitive spec = raw.getAsJsonPrimitive("spec");
    if (spec.isNumber()) {
      counts.numericSpec++;
    } else {
      assertTrue(spec.isString(), "Modern spec must be numeric or a string");
      assertEquals("1", spec.getAsString());
      counts.stringSpec++;
    }
    validateModernInstaller(installer, profile, raw, runtime, counts);
    counts.planned++;
  }

  /** Per-installer integration boundary with the parsed embedded version and full client plan. */
  private static void validateModernInstaller(
      Path installer,
      ModernInstallerProfile profile,
      JsonObject raw,
      IJavaRuntime runtime,
      CorpusCounts counts)
      throws IOException, InterruptedException {
    MinecraftVersionInfo embedded;
    try (Reader reader =
        new InputStreamReader(
            new ByteArrayInputStream(profile.getVersionJsonBytes()), StandardCharsets.UTF_8)) {
      embedded = MojangUtils.getGson().fromJson(reader, MinecraftVersionInfo.class);
    }
    assertNotNull(embedded, "Selected embedded version must deserialize");
    assertEquals(profile.getMinecraft(), embedded.getParentVersion());
    assertEquals(profile.getEmbeddedId(), embedded.getId());
    List<Library> effectiveLibraries = embedded.getLibrariesForCurrentOS(null, runtime);
    ArtifactPlan plan = ModernInstallerArtifactResolver.plan(profile, effectiveLibraries, runtime);
    if (!profile.getClientProcessors().isEmpty()) {
      Path root = Files.createTempDirectory("modern-installer-corpus-");
      try {
        Path cache = Files.createDirectories(root.resolve("cache"));
        Path work = Files.createTempDirectory(cache, "processors-");
        Map<String, String> data =
            InstallerTokenResolver.resolveData(
                profile,
                root,
                installer,
                cache.resolve("minecraft_" + profile.getMinecraft() + ".jar"),
                work);
        for (ModernInstallerProfile.Processor processor : profile.getClientProcessors()) {
          for (String argument : processor.getArgs()) {
            InstallerTokenResolver.resolveArgument(argument, data, root.resolve("libraries"));
          }
          InstallerTokenResolver.resolveOutputs(processor, data, root, root.resolve("libraries"));
        }
      } finally {
        org.apache.commons.io.FileUtils.deleteDirectory(root.toFile());
      }
    }
    Set<String> declaredPaths = libraryPaths(effectiveLibraries, runtime);
    if (!profile.getClientProcessors().isEmpty()) {
      declaredPaths.addAll(libraryPaths(profile.getLibraries(), runtime));
    }
    List<String> executablePaths = new ArrayList<>();
    for (ModernInstallerProfile.Processor processor : profile.getClientProcessors()) {
      executablePaths.add(coordinatePath(processor.getJar(), runtime));
      for (String dependency : processor.getClasspath()) {
        executablePaths.add(coordinatePath(dependency, runtime));
      }
    }
    Set<String> expectedPaths = new LinkedHashSet<>(declaredPaths);
    expectedPaths.addAll(executablePaths);
    Set<String> actualPaths = new LinkedHashSet<>();
    Set<String> inferredPaths = new LinkedHashSet<>();
    for (ArtifactRequest request : plan.getRequests()) {
      assertTrue(
          actualPaths.add(request.getPath()), "Duplicate artifact path: " + request.getPath());
      if (request.isInferred()) {
        assertEquals(
            request.getCoordinate().getPath(),
            request.getPath(),
            "Inferred processor artifacts must have canonical paths");
        inferredPaths.add(request.getPath());
      }
    }
    assertEquals(
        expectedPaths,
        actualPaths,
        "Only selected libraries and client jar/classpath references are acquired");
    Set<String> expectedInferred = new LinkedHashSet<>(executablePaths);
    expectedInferred.removeAll(declaredPaths);
    assertEquals(expectedInferred, inferredPaths, "Only undeclared executable inputs are inferred");
    validateClientDataReferences(profile, plan, expectedPaths, runtime, counts);

    String name = installer.getFileName().toString();
    if (EMPTY_DATA_FIXTURE.equals(name)) {
      assertTrue(raw.get("data").isJsonArray(), "Forge .2851 must exercise data: [] normalization");
      assertEquals(0, raw.getAsJsonArray("data").size());
      assertTrue(profile.getData().isEmpty());
      assertTrue(profile.getClientProcessors().isEmpty());
      String forgePath = coordinatePath("net.minecraftforge:forge:1.12.2-14.23.5.2851", runtime);
      ArtifactRequest forge = plan.getRequest(forgePath);
      assertNotNull(forge, "Zero-client .2851 still needs its embedded game artifact");
      assertTrue(forge.isGameLibrary());
      assertTrue(forge.isEmptyUrl());
      counts.fixtures.add(name);
    }
    if (MISMATCH_FIXTURES.contains(name)) {
      assertNotEquals(
          profile.getVersion(), embedded.getId(), "Fixture must exercise display-ID drift");
      assertEquals("neoforge-20.6.14-beta", profile.getVersion());
      assertEquals(
          name.substring(0, name.length() - "-installer.jar".length()),
          embedded.getId(),
          "The embedded ID, not the stale profile version, must remain authoritative");
      counts.fixtures.add(name);
    }
    if (INFERRED_TOOL_FIXTURES.contains(name)) {
      validateKnownInferredTools(plan, declaredPaths, executablePaths, inferredPaths, runtime);
      counts.fixtures.add(name);
    }
  }

  private static void validateClientDataReferences(
      ModernInstallerProfile profile,
      ArtifactPlan plan,
      Set<String> acquiredPaths,
      IJavaRuntime runtime,
      CorpusCounts counts) {
    for (ModernInstallerProfile.DataValue value : profile.getData().values()) {
      String client = value.getClient();
      if (client == null || !client.startsWith("[") || !client.endsWith("]")) {
        continue;
      }
      MavenCoordinate coordinate =
          MavenCoordinate.parse(
              client.substring(1, client.length() - 1).replace("${arch}", runtime.getBitness()));
      String path = coordinate.getPath();
      assertEquals(
          acquiredPaths.contains(path),
          plan.getRequest(path) != null,
          "Data coordinate must not become an input without a library/jar/classpath declaration: "
              + client);
      if (!"jar".equals(coordinate.getExtension())) {
        counts.nonJarClientData++;
      }
    }
  }

  private static void validateKnownInferredTools(
      ArtifactPlan plan,
      Set<String> declaredPaths,
      List<String> executablePaths,
      Set<String> inferredPaths,
      IJavaRuntime runtime) {
    long undeclaredReferences =
        executablePaths.stream().filter(path -> !declaredPaths.contains(path)).count();
    assertEquals(14L, undeclaredReferences, "Count jar/classpath occurrences before deduplication");
    Set<String> expected = new LinkedHashSet<>();
    for (String tool : EXPECTED_INFERRED_TOOLS) {
      MavenCoordinate coordinate = MavenCoordinate.parse(tool);
      String path = coordinatePath(tool, runtime);
      expected.add(path);
      ArtifactRequest request = plan.getRequest(path);
      assertNotNull(request, "Missing inferred executable: " + tool);
      String repository =
          coordinate.getGroup().startsWith("net.neoforged.")
                  || coordinate.getGroup().equals("net.neoforged")
              ? "https://maven.neoforged.net/releases/"
              : "https://repo.maven.apache.org/maven2/";
      assertEquals(
          Collections.singletonList(repository + path),
          request.getUrls(),
          "Inferred executable must use its group's official repository: " + tool);
    }
    assertEquals(11, inferredPaths.size());
    assertEquals(
        expected, inferredPaths, "Canonical @jar aliases must collapse to the known 11 tools");
  }

  private static Set<String> libraryPaths(List<Library> libraries, IJavaRuntime runtime) {
    Set<String> paths = new LinkedHashSet<>();
    for (Library library : libraries) {
      if (!library.isLocal()) {
        String classifier =
            library.resolveNativeClassifier(
                OperatingSystem.getOperatingSystem().getName(), runtime.getOsArch());
        paths.add(library.getArtifactPath(classifier).replace("${arch}", runtime.getBitness()));
      }
    }
    return paths;
  }

  private static String coordinatePath(String value, IJavaRuntime runtime) {
    return MavenCoordinate.parse(value.replace("${arch}", runtime.getBitness())).getPath();
  }

  private static JsonObject readRawProfile(Path installer) throws IOException {
    try (ZipFile archive = new ZipFile(installer.toFile())) {
      ZipEntry entry = archive.getEntry("install_profile.json");
      if (entry == null) {
        return null;
      }
      try (Reader reader =
          new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
        return JsonParser.parseReader(reader).getAsJsonObject();
      }
    }
  }

  private static IJavaRuntime runtime() {
    return new IJavaRuntime() {
      @Override
      public File getExecutableFile() {
        return new File(System.getProperty("java.home"), "bin/java");
      }

      @Override
      public String getVersion() {
        return System.getProperty("java.version");
      }

      @Override
      public String getVendor() {
        return "corpus-test";
      }

      @Override
      public String getOsArch() {
        return System.getProperty("os.arch");
      }

      @Override
      public String getBitness() {
        return System.getProperty("sun.arch.data.model", "64");
      }

      @Override
      public boolean is64Bit() {
        return "64".equals(getBitness());
      }

      @Override
      public boolean isValid() {
        return true;
      }
    };
  }

  private static final class CorpusCounts {
    private final int total;
    private final Set<String> fixtures = new LinkedHashSet<>();
    private int modern;
    private int legacy;
    private int withoutProfile;
    private int planned;
    private int numericSpec;
    private int stringSpec;
    private int nonJarClientData;

    private CorpusCounts(int total) {
      this.total = total;
    }

    private void requireCoverage(String variable, Path root) {
      Set<String> required = new LinkedHashSet<>();
      if (FORGE_MIRROR.equals(variable)) {
        required.add(EMPTY_DATA_FIXTURE);
      } else {
        required.addAll(MISMATCH_FIXTURES);
        required.addAll(INFERRED_TOOL_FIXTURES);
      }
      required.removeAll(fixtures);
      assertAll(
          "Required corpus coverage at " + root,
          () -> assertTrue(required.isEmpty(), "Missing or invalid required fixtures: " + required),
          () ->
              assertEquals(
                  total, modern + legacy + withoutProfile, "Every installer is classified"),
          () ->
              assertEquals(modern, planned, "Every modern installer has a validated artifact plan"),
          () -> assertTrue(numericSpec > 0, "Missing numeric-spec profiles"),
          () ->
              assertTrue(
                  !NEOFORGE_MIRROR.equals(variable) || stringSpec > 0,
                  "Missing historical NeoForge string-spec profiles"),
          () -> assertTrue(nonJarClientData > 0, "Missing non-JAR client data coordinates"));
    }

    @Override
    public String toString() {
      return "installers="
          + total
          + ", modern="
          + modern
          + ", legacy="
          + legacy
          + ", without-profile="
          + withoutProfile
          + ", validated-plans="
          + planned
          + ", numeric-spec="
          + numericSpec
          + ", string-spec="
          + stringSpec
          + ", non-jar-client-data="
          + nonJarClientData
          + ", required-fixtures="
          + fixtures;
    }
  }
}
