package net.technicpack.launcher.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.install.plan.PlanExecutor;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimesIndex;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.AssetIndex;
import net.technicpack.minecraftcore.mojang.version.io.GameDownloads;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.ReleaseType;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import net.technicpack.minecraftcore.mojang.version.io.VersionJavaInfo;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.rest.io.Modpack;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.JavaUtils;
import net.technicpack.utilslib.OSUtils;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImmutableInstallerPlannerTest {
  private static final Gson GSON = new Gson();

  @TempDir Path tempDir;

  @Test
  void throwIfInterruptedThrowsWhenCurrentThreadIsInterrupted() {
    try {
      Thread.currentThread().interrupt();
      assertThrows(InterruptedException.class, ImmutableInstallerPlanner::throwIfInterrupted);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void throwIfCancelledThrowsWhenCancellationFlagIsSet() {
    assertThrows(
        InterruptedException.class, () -> ImmutableInstallerPlanner.throwIfCancelled(() -> true));
  }

  @Test
  void preparationPlanAppliesModpackArchivesInLegacyQueueOrder() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-root"));
    ModpackModel pack =
        new ModpackModel(
            new InstalledPack("Test Pack", "1.0", InstalledPack.MODPACKS_DIR + "Test Pack"),
            null,
            null,
            fileSystem);
    pack.initDirectories();

    writeZip(pack.getCacheDir().toPath().resolve("mod-a-1.0.zip"), "overlap.txt", "first");
    writeZip(pack.getCacheDir().toPath().resolve("mod-b-1.0.zip"), "overlap.txt", "second");

    Modpack modpackData =
        GSON.fromJson(
            "{"
                + "\"minecraft\":\"1.7.10\","
                + "\"mods\":["
                + "{\"name\":\"mod-a\",\"version\":\"1.0\",\"url\":\"https://example.invalid/mod-a.zip\",\"md5\":\"\"},"
                + "{\"name\":\"mod-b\",\"version\":\"1.0\",\"url\":\"https://example.invalid/mod-b.zip\",\"md5\":\"\"}"
                + "]"
                + "}",
            Modpack.class);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            modpackData,
            fileSystem,
            null,
            new TechnicSettings(),
            null,
            true,
            false,
            false,
            () -> false);

    ExecutionPlan<ImmutableInstallerPlanner.InstallExecutionContext> plan =
        planner.buildPreparationPlan();
    new PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext>(null)
        .execute(plan, new ImmutableInstallerPlanner.InstallExecutionContext());

    assertEquals(
        "first",
        Files.readString(pack.getInstalledDirectory().toPath().resolve("overlap.txt")),
        "legacy queueing extracted later-listed archives first, so the first listed archive won conflicts");
  }

  @Test
  void installJavaRuntimeDoesNotCreateLinksBeforeFileDownloadsSucceed() throws Exception {
    Assumptions.assumeTrue(
        OperatingSystem.getOperatingSystem() != OperatingSystem.WINDOWS,
        "link creation semantics in this regression test are only reliable on unix-like systems");

    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-root"));
    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            new ModpackModel(
                new InstalledPack("Test Pack", "1.0", InstalledPack.MODPACKS_DIR + "Test Pack"),
                null,
                null,
                fileSystem),
            GSON.fromJson("{\"minecraft\":\"1.20.1\",\"mods\":[]}", Modpack.class),
            fileSystem,
            null,
            new TechnicSettings(),
            null,
            false,
            true,
            false,
            () -> false);

    byte[] runtimeFileBytes = "runtime".getBytes(StandardCharsets.UTF_8);
    byte[] manifestBytes;

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      server.createContext("/runtime.bin", exchange -> respond(exchange, 404, new byte[0]));

      String runtimeUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/runtime.bin";
      manifestBytes =
          ("{"
                  + "\"files\":{"
                  + "\"bin\":{\"type\":\"directory\"},"
                  + "\"bin/java-link\":{\"type\":\"link\",\"target\":\"java-target\"},"
                  + "\"bin/java-target\":{"
                  + "\"type\":\"file\","
                  + "\"downloads\":{\"raw\":{\"sha1\":\""
                  + sha1(runtimeFileBytes)
                  + "\",\"size\":"
                  + runtimeFileBytes.length
                  + ",\"url\":\""
                  + runtimeUrl
                  + "\"}},"
                  + "\"executable\":false"
                  + "}"
                  + "}"
                  + "}")
              .getBytes(StandardCharsets.UTF_8);
      server.createContext("/runtime.json", exchange -> respond(exchange, 200, manifestBytes));
      server.start();

      JavaRuntimesIndex previousIndex = setJavaRuntimesIndex(server, manifestBytes);
      try {
        VersionJavaInfo runtimeInfo =
            GSON.fromJson(
                "{\"component\":\"test-runtime\",\"majorVersion\":17}", VersionJavaInfo.class);
        TestMinecraftVersionInfo version = new TestMinecraftVersionInfo(runtimeInfo);
        ImmutableInstallerPlanner.InstallExecutionContext context =
            new ImmutableInstallerPlanner.InstallExecutionContext();
        context.setResolvedVersion(version);
        TechnicConstants.setBuildNumber(() -> "0");

        Path linkPath = fileSystem.getRuntimesDirectory().resolve("test-runtime/bin/java-link");
        assertThrows(
            DownloadException.class,
            () ->
                invokeInstallJavaRuntime(
                    planner, context, new RecordingReporter(new ArrayList<>())));
        assertFalse(
            Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS),
            "legacy runtime install only created links after file downloads had already succeeded");
      } finally {
        setJavaRuntimesIndex(previousIndex);
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  void versionDiscoveryKeepsNativeAndClasspathLibrariesWithSameCoordinates() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher"));
    InstalledPack installedPack =
        new InstalledPack(
            "test-pack", InstalledPack.RECOMMENDED, tempDir.resolve("pack").toString());
    ModpackModel pack = new ModpackModel(installedPack, null, null, fileSystem);
    pack.initDirectories();

    Modpack modpackData = GSON.fromJson("{\"minecraft\":\"1.16.5\",\"mods\":[]}", Modpack.class);
    IMinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"test-pack\","
                    + "\"type\":\"release\","
                    + "\"mainClass\":\"example.Main\","
                    + "\"inheritsFrom\":\"1.16.5\","
                    + "\"minecraftArguments\":\"--demo\","
                    + "\"libraries\":["
                    + "{\"name\":\"org.lwjgl:lwjgl:3.2.2\"},"
                    + "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"natives\":{"
                    + "\"linux\":\"natives-linux\","
                    + "\"windows\":\"natives-windows\","
                    + "\"osx\":\"natives-macos\""
                    + "}"
                    + "}"
                    + "]"
                    + "}",
                MinecraftVersionInfo.class);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            modpackData,
            fileSystem,
            key -> version,
            new TechnicSettings(),
            new FakeJavaRuntime(),
            false,
            false,
            false,
            () -> false);
    ImmutableInstallerPlanner.InstallExecutionContext context =
        new ImmutableInstallerPlanner.InstallExecutionContext();

    new PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext>(null)
        .execute(planner.buildVersionDiscoveryPlan(), context);

    List<Library> librariesToInstall = context.getLibrariesToInstall();
    assertEquals(2, librariesToInstall.size());
    assertEquals(
        1,
        librariesToInstall.stream()
            .filter(library -> library.getName().equals("org.lwjgl:lwjgl:3.2.2"))
            .filter(library -> library.getNatives() == null)
            .count());
    assertEquals(
        1,
        librariesToInstall.stream()
            .filter(library -> library.getName().equals("org.lwjgl:lwjgl:3.2.2"))
            .filter(library -> library.getNatives() != null)
            .count());
  }

  @Test
  void versionDiscoveryKeepsNativeEntriesThatNeedPlainArtifactFallback() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-fallback"));
    InstalledPack installedPack =
        new InstalledPack(
            "fallback-pack",
            InstalledPack.RECOMMENDED,
            tempDir.resolve("pack-fallback").toString());
    ModpackModel pack = new ModpackModel(installedPack, null, null, fileSystem);
    pack.initDirectories();

    Modpack modpackData = GSON.fromJson("{\"minecraft\":\"1.16.5\",\"mods\":[]}", Modpack.class);
    IMinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"fallback-pack\","
                    + "\"type\":\"release\","
                    + "\"mainClass\":\"example.Main\","
                    + "\"inheritsFrom\":\"1.16.5\","
                    + "\"minecraftArguments\":\"--demo\","
                    + "\"libraries\":["
                    + "{"
                    + "\"name\":\"com.mojang:text2speech:1.11.3\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-linux\":{\"sha1\":\"native-linux\",\"size\":1,\"url\":\"https://example/native-linux.jar\"},"
                    + "\"natives-windows\":{\"sha1\":\"native-win\",\"size\":1,\"url\":\"https://example/native-win.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\"linux\":\"natives-linux\",\"windows\":\"natives-windows\"}"
                    + "},"
                    + "{"
                    + "\"name\":\"com.mojang:text2speech:1.11.3\","
                    + "\"downloads\":{\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"}}"
                    + "}"
                    + "]"
                    + "}",
                MinecraftVersionInfo.class);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            modpackData,
            fileSystem,
            key -> version,
            new TechnicSettings(),
            new FakeJavaRuntime(),
            false,
            false,
            false,
            () -> false);
    ImmutableInstallerPlanner.InstallExecutionContext context =
        new ImmutableInstallerPlanner.InstallExecutionContext();

    new PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext>(null)
        .execute(planner.buildVersionDiscoveryPlan(), context);

    assertEquals(2, context.getLibrariesToInstall().size());
  }

  @Test
  void installVersionLibraryPrefersArm64NativeCacheForArm64Runtime() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-arm64"));
    InstalledPack installedPack =
        new InstalledPack(
            "arm64-pack",
            InstalledPack.RECOMMENDED,
            tempDir.resolve("pack-arm64").toString());
    ModpackModel pack = new ModpackModel(installedPack, null, null, fileSystem);
    pack.initDirectories();

    String currentOs = OperatingSystem.getOperatingSystem().getName();
    String arm64Key = currentOs + "-arm64";
    String genericClassifier = "natives-generic";
    String arm64Classifier = "natives-arm64";

    Path genericZip = tempDir.resolve("generic-native.zip");
    Path arm64Zip = tempDir.resolve("arm64-native.zip");
    writeZip(genericZip, "marker.txt", "generic");
    writeZip(arm64Zip, "marker.txt", "arm64");
    byte[] genericBytes = Files.readAllBytes(genericZip);
    byte[] arm64Bytes = Files.readAllBytes(arm64Zip);

    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"classifiers\":{"
                    + "\""
                    + genericClassifier
                    + "\":{\"sha1\":\""
                    + sha1(genericBytes)
                    + "\",\"size\":"
                    + genericBytes.length
                    + ",\"url\":\"https://example/generic.jar\"},"
                    + "\""
                    + arm64Classifier
                    + "\":{\"sha1\":\""
                    + sha1(arm64Bytes)
                    + "\",\"size\":"
                    + arm64Bytes.length
                    + ",\"url\":\"https://example/arm64.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\""
                    + currentOs
                    + "\":\""
                    + genericClassifier
                    + "\",\""
                    + arm64Key
                    + "\":\""
                    + arm64Classifier
                    + "\"}"
                    + "}",
                Library.class);

    Path genericCache = fileSystem.getCacheDirectory().resolve(library.getArtifactPath(genericClassifier));
    Path arm64Cache = fileSystem.getCacheDirectory().resolve(library.getArtifactPath(arm64Classifier));
    Files.createDirectories(genericCache.getParent());
    Files.createDirectories(arm64Cache.getParent());
    Files.write(genericCache, genericBytes);
    Files.write(arm64Cache, arm64Bytes);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            GSON.fromJson("{\"minecraft\":\"1.16.5\",\"mods\":[]}", Modpack.class),
            fileSystem,
            null,
            new TechnicSettings(),
            new FakeJavaRuntime("aarch64"),
            false,
            false,
            false,
            () -> false);
    ImmutableInstallerPlanner.InstallExecutionContext context =
        new ImmutableInstallerPlanner.InstallExecutionContext();
    TestMinecraftVersionInfo version = new TestMinecraftVersionInfo(null);
    version.setJavaRuntime(new FakeJavaRuntime("aarch64"));
    context.setResolvedVersion(version);

    invokeInstallVersionLibrary(planner, context, library, new RecordingReporter(new ArrayList<>()));

    Path extractedMarker = pack.getBinDir().toPath().resolve("natives/marker.txt");
    assertTrue(Files.exists(extractedMarker));
    assertEquals("arm64", Files.readString(extractedMarker, StandardCharsets.UTF_8));
  }

  @Test
  void mergeLibraryWithDifferentClassifierDoesNotReplace() throws Exception {
    TestMinecraftVersionInfo version = new TestMinecraftVersionInfo(null);
    version.addLibrary(new Library("org.lwjgl:lwjgl:3.3.1:natives-windows"));

    invokeMergeLibrary(version, new Library("org.lwjgl:lwjgl:3.3.1:natives-linux"));

    List<String> names =
        version.getLibraries().stream().map(Library::getName).collect(Collectors.toList());
    assertEquals(
        Arrays.asList(
            "org.lwjgl:lwjgl:3.3.1:natives-windows", "org.lwjgl:lwjgl:3.3.1:natives-linux"),
        names);
  }

  @Test
  void mergeLibraryWithSameClassifierAndHigherVersionReplaces() throws Exception {
    TestMinecraftVersionInfo version = new TestMinecraftVersionInfo(null);
    version.addLibrary(new Library("org.lwjgl:lwjgl:3.3.1:natives-windows"));

    invokeMergeLibrary(version, new Library("org.lwjgl:lwjgl:3.3.2:natives-windows"));

    List<String> names =
        version.getLibraries().stream().map(Library::getName).collect(Collectors.toList());
    assertEquals(Collections.singletonList("org.lwjgl:lwjgl:3.3.2:natives-windows"), names);
  }

  @Test
  void mergeLibraryWithNoClassifierAgainstClassifiedExistingCoexists() throws Exception {
    TestMinecraftVersionInfo version = new TestMinecraftVersionInfo(null);
    version.addLibrary(new Library("org.lwjgl:lwjgl:3.3.1:natives-linux"));

    invokeMergeLibrary(version, new Library("org.lwjgl:lwjgl:3.3.2"));

    List<String> names =
        version.getLibraries().stream().map(Library::getName).collect(Collectors.toList());
    assertEquals(
        Arrays.asList("org.lwjgl:lwjgl:3.3.1:natives-linux", "org.lwjgl:lwjgl:3.3.2"), names);
  }

  @Test
  void versionDiscoveryRemovesLegacyForgeWhenModpackJarPresent() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-forge-with-jar"));
    InstalledPack installedPack =
        new InstalledPack(
            "forge-with-jar",
            InstalledPack.RECOMMENDED,
            tempDir.resolve("pack-forge-with-jar").toString());
    ModpackModel pack = new ModpackModel(installedPack, null, null, fileSystem);
    pack.initDirectories();

    Path modpackJar = pack.getBinDir().toPath().resolve("modpack.jar");
    Files.createDirectories(modpackJar.getParent());
    Files.write(modpackJar, new byte[] {0x50, 0x4b, 0x03, 0x04});

    Modpack modpackData = GSON.fromJson("{\"minecraft\":\"1.7.10\",\"mods\":[]}", Modpack.class);
    IMinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"forge-with-jar-1.7.10\","
                    + "\"type\":\"release\","
                    + "\"mainClass\":\"example.Main\","
                    + "\"inheritsFrom\":\"1.7.10\","
                    + "\"minecraftArguments\":\"--demo\","
                    + "\"libraries\":["
                    + "{\"name\":\"net.minecraftforge:forge:1.7.10-10.13.4.1614\"}"
                    + "]"
                    + "}",
                MinecraftVersionInfo.class);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            modpackData,
            fileSystem,
            key -> version,
            new TechnicSettings(),
            new FakeJavaRuntime(),
            false,
            false,
            false,
            () -> false);
    ImmutableInstallerPlanner.InstallExecutionContext context =
        new ImmutableInstallerPlanner.InstallExecutionContext();

    new PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext>(null)
        .execute(planner.buildVersionDiscoveryPlan(), context);

    assertTrue(
        context.getLibrariesToInstall().stream().noneMatch(Library::isMinecraftForge),
        "Forge library must be removed when modpack.jar provides Forge classes on the classpath");
  }

  @Test
  void versionDiscoveryKeepsLegacyForgeWhenModpackJarAbsent() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-forge-no-jar"));
    InstalledPack installedPack =
        new InstalledPack(
            "forge-no-jar",
            InstalledPack.RECOMMENDED,
            tempDir.resolve("pack-forge-no-jar").toString());
    ModpackModel pack = new ModpackModel(installedPack, null, null, fileSystem);
    pack.initDirectories();

    assertFalse(
        new File(pack.getBinDir(), "modpack.jar").exists(),
        "precondition: modpack.jar must not exist for this test");

    Modpack modpackData = GSON.fromJson("{\"minecraft\":\"1.7.10\",\"mods\":[]}", Modpack.class);
    IMinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"forge-no-jar-1.7.10\","
                    + "\"type\":\"release\","
                    + "\"mainClass\":\"example.Main\","
                    + "\"inheritsFrom\":\"1.7.10\","
                    + "\"minecraftArguments\":\"--demo\","
                    + "\"libraries\":["
                    + "{\"name\":\"net.minecraftforge:forge:1.7.10-10.13.4.1614\"}"
                    + "]"
                    + "}",
                MinecraftVersionInfo.class);

    ImmutableInstallerPlanner planner =
        new ImmutableInstallerPlanner(
            new TestResourceLoader(),
            pack,
            modpackData,
            fileSystem,
            key -> version,
            new TechnicSettings(),
            new FakeJavaRuntime(),
            false,
            false,
            false,
            () -> false);
    ImmutableInstallerPlanner.InstallExecutionContext context =
        new ImmutableInstallerPlanner.InstallExecutionContext();

    new PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext>(null)
        .execute(planner.buildVersionDiscoveryPlan(), context);

    assertEquals(
        1,
        context.getLibrariesToInstall().stream().filter(Library::isMinecraftForge).count(),
        "Forge library must be retained when modpack.jar is absent so a Prism patch can supply"
            + " Forge classes via the chain");
  }

  @Test
  void deriveJavaVersionPicksHighestComponentNotExceedingRequestedFromManifest() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-derive-1"));
    ImmutableInstallerPlanner planner = makeMinimalPlanner(fileSystem);

    JavaRuntimesIndex index =
        MojangUtils.getGson()
            .fromJson(
                "{\""
                    + currentRuntimeIndexKey()
                    + "\":{"
                    + "\"java-runtime-delta\":[{\"version\":{\"name\":\"21.0.4\",\"released\":\"2024\"}}],"
                    + "\"java-runtime-epsilon\":[{\"version\":{\"name\":\"25.0.0\",\"released\":\"2024\"}}]"
                    + "}}",
                JavaRuntimesIndex.class);
    JavaRuntimesIndex previous = setJavaRuntimesIndex(index);
    try {
      VersionJavaInfo result = invokeDeriveJavaVersion(planner, Collections.singletonList(22));
      assertEquals("java-runtime-delta", result.getComponent());
      assertEquals(21, result.getMajorVersion());
    } finally {
      setJavaRuntimesIndex(previous);
    }
  }

  @Test
  void deriveJavaVersionPicksExactMatchFromManifest() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-derive-2"));
    ImmutableInstallerPlanner planner = makeMinimalPlanner(fileSystem);

    JavaRuntimesIndex index =
        MojangUtils.getGson()
            .fromJson(
                "{\""
                    + currentRuntimeIndexKey()
                    + "\":{"
                    + "\"java-runtime-epsilon\":[{\"version\":{\"name\":\"25.0.0\",\"released\":\"2024\"}}]"
                    + "}}",
                JavaRuntimesIndex.class);
    JavaRuntimesIndex previous = setJavaRuntimesIndex(index);
    try {
      VersionJavaInfo result = invokeDeriveJavaVersion(planner, Collections.singletonList(25));
      assertEquals("java-runtime-epsilon", result.getComponent());
      assertEquals(25, result.getMajorVersion());
    } finally {
      setJavaRuntimesIndex(previous);
    }
  }

  @Test
  void deriveJavaVersionSkipsComponentsWithUnrecognizedVersionStrings() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher-derive-3"));
    ImmutableInstallerPlanner planner = makeMinimalPlanner(fileSystem);

    JavaRuntimesIndex index =
        MojangUtils.getGson()
            .fromJson(
                "{\""
                    + currentRuntimeIndexKey()
                    + "\":{"
                    + "\"java-runtime-delta\":[{\"version\":{\"name\":\"21.0.4\",\"released\":\"2024\"}}],"
                    + "\"java-runtime-bogus\":[{\"version\":{\"name\":\"weird-format\",\"released\":\"2024\"}}]"
                    + "}}",
                JavaRuntimesIndex.class);
    JavaRuntimesIndex previous = setJavaRuntimesIndex(index);
    try {
      VersionJavaInfo result = invokeDeriveJavaVersion(planner, Collections.singletonList(25));
      assertEquals("java-runtime-delta", result.getComponent());
      assertEquals(21, result.getMajorVersion());
    } finally {
      setJavaRuntimesIndex(previous);
    }
  }

  private ImmutableInstallerPlanner makeMinimalPlanner(LauncherFileSystem fileSystem) {
    return new ImmutableInstallerPlanner(
        new TestResourceLoader(),
        new ModpackModel(
            new InstalledPack("Test Pack", "1.0", InstalledPack.MODPACKS_DIR + "Test Pack"),
            null,
            null,
            fileSystem),
        GSON.fromJson("{\"minecraft\":\"1.20.1\",\"mods\":[]}", Modpack.class),
        fileSystem,
        null,
        new TechnicSettings(),
        null,
        false,
        true,
        false,
        () -> false);
  }

  private static VersionJavaInfo invokeDeriveJavaVersion(
      ImmutableInstallerPlanner planner, List<Integer> majors) throws Exception {
    Method method =
        ImmutableInstallerPlanner.class.getDeclaredMethod(
            "deriveJavaVersionFromCompatibleMajors", List.class);
    method.setAccessible(true);
    try {
      return (VersionJavaInfo) method.invoke(planner, majors);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw exception;
    }
  }

  private static void invokeMergeLibrary(IMinecraftVersionInfo version, Library newLib)
      throws Exception {
    Method method =
        ImmutableInstallerPlanner.class.getDeclaredMethod(
            "mergeLibrary", IMinecraftVersionInfo.class, Library.class);
    method.setAccessible(true);
    try {
      method.invoke(null, version, newLib);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw exception;
    }
  }

  private static void writeZip(Path zipPath, String entryName, String contents) throws IOException {
    Files.createDirectories(zipPath.getParent());
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      output.putNextEntry(new ZipEntry(entryName));
      output.write(contents.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
    }
  }

  private static void respond(HttpExchange exchange, int statusCode, byte[] body)
      throws IOException {
    exchange.sendResponseHeaders(statusCode, body.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(body);
    }
  }

  private static String sha1(byte[] bytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    byte[] hash = digest.digest(bytes);
    StringBuilder builder = new StringBuilder(hash.length * 2);
    for (byte value : hash) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private static JavaRuntimesIndex setJavaRuntimesIndex(HttpServer server, byte[] manifestBytes)
      throws Exception {
    String manifestUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/runtime.json";
    String indexJson =
        "{"
            + "\""
            + currentRuntimeIndexKey()
            + "\":{"
            + "\"test-runtime\":[{"
            + "\"manifest\":{"
            + "\"sha1\":\""
            + sha1(manifestBytes)
            + "\",\"size\":"
            + manifestBytes.length
            + ",\"url\":\""
            + manifestUrl
            + "\""
            + "}"
            + "}]"
            + "}"
            + "}";
    JavaRuntimesIndex index = MojangUtils.getGson().fromJson(indexJson, JavaRuntimesIndex.class);
    return setJavaRuntimesIndex(index);
  }

  private static JavaRuntimesIndex setJavaRuntimesIndex(JavaRuntimesIndex index) throws Exception {
    Field field = MojangUtils.class.getDeclaredField("javaRuntimesIndex");
    field.setAccessible(true);
    JavaRuntimesIndex previous = (JavaRuntimesIndex) field.get(null);
    field.set(null, index);
    return previous;
  }

  private static String currentRuntimeIndexKey() {
    switch (OperatingSystem.getOperatingSystem()) {
      case LINUX:
        return OSUtils.is64BitOS() ? "linux" : "linux-i386";
      case OSX:
        return JavaUtils.isArm64() ? "mac-os-arm64" : "mac-os";
      case WINDOWS:
        if (JavaUtils.isArm64()) {
          return "windows-arm64";
        }
        return OSUtils.is64BitOS() ? "windows-x64" : "windows-x86";
      case UNKNOWN:
      default:
        throw new IllegalStateException("Unsupported operating system for runtime manifest test");
    }
  }

  private static void invokeInstallJavaRuntime(
      ImmutableInstallerPlanner planner,
      ImmutableInstallerPlanner.InstallExecutionContext context,
      NodeProgressReporter reporter)
      throws Exception {
    Method method =
        ImmutableInstallerPlanner.class.getDeclaredMethod(
            "installJavaRuntime",
            ImmutableInstallerPlanner.InstallExecutionContext.class,
            NodeProgressReporter.class);
    method.setAccessible(true);
    try {
      method.invoke(planner, context, reporter);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw exception;
    }
  }

  private static void invokeInstallVersionLibrary(
      ImmutableInstallerPlanner planner,
      ImmutableInstallerPlanner.InstallExecutionContext context,
      Library library,
      NodeProgressReporter reporter)
      throws Exception {
    Method method =
        ImmutableInstallerPlanner.class.getDeclaredMethod(
            "installVersionLibrary",
            ImmutableInstallerPlanner.InstallExecutionContext.class,
            Library.class,
            NodeProgressReporter.class);
    method.setAccessible(true);
    try {
      method.invoke(planner, context, library, reporter);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw exception;
    }
  }

  private static final class TestResourceLoader extends ResourceLoader {
    private TestResourceLoader() {
      super((LauncherFileSystem) null, "net", "technicpack", "launcher");
    }

    @Override
    public String getString(String stringKey, String... replacements) {
      return stringKey;
    }
  }

  private static final class RecordingReporter implements NodeProgressReporter {
    private final List<String> events;

    private RecordingReporter(List<String> events) {
      this.events = events;
    }

    @Override
    public void updateNodeProgress(float percent) {}

    @Override
    public void updateCurrentItem(
        String label, net.technicpack.launchercore.progress.CurrentItemMode mode, Float percent) {
      if ("Linking files.".equals(label)) {
        events.add("link");
      } else if (label != null && label.contains("java-target")) {
        events.add("file");
      }
    }
  }

  private static final class TestMinecraftVersionInfo implements IMinecraftVersionInfo {
    private final VersionJavaInfo runtimeInfo;
    private final List<Library> libraries = new ArrayList<>();
    private IJavaRuntime runtime;

    private TestMinecraftVersionInfo(VersionJavaInfo runtimeInfo) {
      this.runtimeInfo = runtimeInfo;
    }

    @Override
    public String getId() {
      return "test-version";
    }

    @Override
    public ReleaseType getType() {
      return null;
    }

    @Override
    public ArgumentList getMinecraftArguments() {
      return null;
    }

    @Override
    public ArgumentList getJavaArguments() {
      return null;
    }

    @Override
    public ArgumentList getDefaultUserJavaArguments() {
      return null;
    }

    @Override
    public List<Library> getLibraries() {
      return libraries;
    }

    @Override
    public List<Library> getLibrariesForCurrentOS(ILaunchOptions options, IJavaRuntime runtime) {
      return Collections.emptyList();
    }

    @Override
    public String getMainClass() {
      return null;
    }

    @Override
    public void setMainClass(String mainClass) {}

    @Override
    public List<Rule> getRules() {
      return Collections.emptyList();
    }

    @Override
    public String getAssetsKey() {
      return null;
    }

    @Override
    public AssetIndex getAssetIndex() {
      return null;
    }

    @Override
    public GameDownloads getDownloads() {
      return null;
    }

    @Override
    public String getParentVersion() {
      return "1.20.1";
    }

    @Override
    public boolean getAreAssetsVirtual() {
      return false;
    }

    @Override
    public void setAreAssetsVirtual(boolean areAssetsVirtual) {}

    @Override
    public boolean getAssetsMapToResources() {
      return false;
    }

    @Override
    public void setAssetsMapToResources(boolean mapToResources) {}

    @Override
    public void addLibrary(Library library) {
      libraries.add(library);
    }

    @Override
    public void prependLibrary(Library library) {
      libraries.add(0, library);
    }

    @Override
    public VersionJavaInfo getMojangRuntimeInformation() {
      return runtimeInfo;
    }

    @Override
    public void removeLibrary(String libraryName) {
      libraries.removeIf(library -> library.getName().equals(libraryName));
    }

    @Override
    public void replaceAllLibraries(List<Library> replacementLibraries) {
      libraries.clear();
      libraries.addAll(replacementLibraries);
    }

    @Override
    public void setMojangRuntimeInformation(VersionJavaInfo info) {}

    @Override
    public void addJvmArguments(List<Argument> args) {}

    @Override
    public void addGameArguments(List<Argument> args) {}

    @Override
    public IJavaRuntime getJavaRuntime() {
      return runtime;
    }

    @Override
    public void setJavaRuntime(IJavaRuntime runtime) {
      this.runtime = runtime;
    }
  }

  private static final class FakeJavaRuntime implements IJavaRuntime {
    private final String osArch;

    private FakeJavaRuntime() {
      this("amd64");
    }

    private FakeJavaRuntime(String osArch) {
      this.osArch = osArch;
    }

    @Override
    public File getExecutableFile() {
      return new File("java");
    }

    @Override
    public String getVersion() {
      return "17";
    }

    @Override
    public String getVendor() {
      return "Test";
    }

    @Override
    public String getOsArch() {
      return osArch;
    }

    @Override
    public String getBitness() {
      return osArch.contains("64") ? "64" : "32";
    }

    @Override
    public boolean is64Bit() {
      return getBitness().equals("64");
    }

    @Override
    public boolean isValid() {
      return true;
    }
  }
}
