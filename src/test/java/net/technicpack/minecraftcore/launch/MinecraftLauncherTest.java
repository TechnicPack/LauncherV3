package net.technicpack.minecraftcore.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.chain.ChainedMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.utilslib.OperatingSystem;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MinecraftLauncherTest {
  @TempDir Path tempDir;

  @ParameterizedTest
  @ValueSource(strings = {"1.12.2-forge-14.23.5.2851", "1.17.1-forge-37.1.1", "neoforge-26.2.0.75"})
  void buildsNativeLoaderCommandUsingPreparedAliasAndOriginalMetadata(String versionId)
      throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("configured root"));
    ModpackModel pack = createPack(fileSystem);
    ChainedMinecraftVersionInfo version = nativeVersion(versionId);
    Path libraries = fileSystem.getLibrariesDirectory();
    Path bootstrap = writeFile(libraries.resolve("loader/bootstrap/1/bootstrap-1.jar"));
    Path modules = writeFile(libraries.resolve("declared/modules.jar"));
    Path local = writeFile(pack.getInstalledDirectory().toPath().resolve("libraries/patch-1.jar"));
    Path canonical = writeFile(fileSystem.getCacheDirectory().resolve("minecraft_1.17.1.jar"));
    Path alias = MojangUtils.getModernLaunchJar(pack.getBinDir().toPath(), versionId);
    Files.createDirectories(alias.getParent());
    Files.copy(canonical, alias);
    Path installer = writeFile(pack.getBinDir().toPath().resolve("modpack.jar"));
    writeFile(pack.getBinDir().toPath().resolve("minecraft.jar"));

    List<String> commands = buildCommands(fileSystem, pack, version);

    assertEquals(
        List.of(bootstrap.toString(), modules.toString(), local.toString(), alias.toString()),
        Arrays.asList(valueAfter(commands, "-cp").split(Pattern.quote(File.pathSeparator))));
    assertEquals(
        List.of(bootstrap, modules),
        Arrays.stream(
                valueAfter(commands, "--module-path").split(Pattern.quote(File.pathSeparator)))
            .map(Path::of)
            .map(Path::normalize)
            .collect(Collectors.toList()));
    assertTrue(commands.contains("-DlibraryDirectory=" + libraries));
    assertFalse(commands.stream().anyMatch(argument -> argument.contains("${")));
    assertFalse(
        commands.stream().anyMatch(argument -> argument.toLowerCase().contains("forgewrapper")));
    assertFalse(commands.stream().anyMatch(argument -> argument.contains(installer.toString())));
    assertEquals(
        "cpw.mods.bootstraplauncher.BootstrapLauncher",
        commands.get(commands.indexOf("--version") - 1));
    assertEquals(versionId, valueAfter(commands, "--version"));
    assertEquals("OfflinePlayer", valueAfter(commands, "--username"));
    assertEquals("0", valueAfter(commands, "--accessToken"));
    List<String> ignoreLists = properties(commands, "-DignoreList=");
    String ignoreList = ignoreLists.get(ignoreLists.size() - 1).substring("-DignoreList=".length());
    assertTrue(
        Arrays.stream(ignoreList.split(","))
            .anyMatch(prefix -> alias.getFileName().toString().startsWith(prefix)));
    assertFalse(
        Arrays.stream(ignoreList.split(","))
            .anyMatch(prefix -> canonical.getFileName().toString().startsWith(prefix)));
    assertEquals(
        List.of("-Dloader.property=parent", "-Dloader.property=child", "-Dloader.property=prism"),
        properties(commands, "-Dloader.property="));
    assertEquals("child", valueAfter(commands, "--launchTarget"));
    assertEquals(1, Collections.frequency(commands, "--launchTarget"));
    assertEquals(
        List.of("child.Tweaker", "parent.Tweaker"),
        IntStream.range(0, commands.size() - 1)
            .filter(index -> commands.get(index).equals("--tweakClass"))
            .mapToObj(index -> commands.get(index + 1))
            .collect(Collectors.toList()));
  }

  @Test
  void missingPreparedAliasFailsWithoutCopyingCanonicalVanillaAtLaunch() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher"));
    ModpackModel pack = createPack(fileSystem);
    MinecraftVersionInfo version =
        version(
            "{\"id\":\"neoforge-26.2.0.75\",\"inheritsFrom\":\"26.2\","
                + "\"mainClass\":\"net.neoforged.fml.startup.Client\","
                + "\"type\":\"release\",\"libraries\":[],\"arguments\":{\"game\":[],\"jvm\":[]}}");
    version.setJavaRuntime(new FakeJavaRuntime());
    Path canonical = writeFile(fileSystem.getCacheDirectory().resolve("minecraft_26.2.jar"));
    Path alias = MojangUtils.getModernLaunchJar(pack.getBinDir().toPath(), version.getId());
    writeFile(pack.getBinDir().toPath().resolve("minecraft.jar"));

    assertThrows(RuntimeException.class, () -> buildCommands(fileSystem, pack, version));

    assertFalse(Files.exists(alias.getParent()));
    assertTrue(Files.isRegularFile(canonical));
  }

  @Test
  void legacyLaunchRetainsPackJarsAndVersionJvmArguments() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher"));
    ModpackModel pack = createPack(fileSystem);
    MinecraftVersionInfo version =
        version(
            "{\"id\":\"1.7.10-Forge10.13.4.1614-1.7.10\",\"type\":\"release\","
                + "\"mainClass\":\"net.minecraft.launchwrapper.Launch\",\"libraries\":[],"
                + "\"arguments\":{\"game\":[\"--version\",\"${version_name}\"],"
                + "\"jvm\":[\"-Dlegacy.option=kept\"]}}");
    version.setJavaRuntime(new FakeJavaRuntime());
    Path installer = writeFile(pack.getBinDir().toPath().resolve("modpack.jar"));
    Path minecraft = writeFile(pack.getBinDir().toPath().resolve("minecraft.jar"));

    List<String> commands = buildCommands(fileSystem, pack, version);

    assertEquals(installer + File.pathSeparator + minecraft, valueAfter(commands, "-cp"));
    assertTrue(commands.contains("-Dlegacy.option=kept"));
    assertTrue(commands.contains("net.minecraft.launchwrapper.Launch"));
    assertEquals(version.getId(), valueAfter(commands, "--version"));
    assertFalse(Files.exists(pack.getBinDir().toPath().resolve("native-launch")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        ".",
        "..",
        "../outside",
        "loader/child",
        "loader\\child",
        "/absolute",
        "C:relative",
        "C:\\absolute",
        "\\\\server\\share",
        "bad\u0000id",
        "bad\nid",
        "bad\u007fid",
        "bad:id",
        "bad;id",
        "bad*id",
        "bad?id",
        "bad\"id",
        "bad<id",
        "bad>id",
        "bad|id",
        "trailing.",
        "trailing ",
        "CON",
        "nul.jar",
        "LPT9"
      })
  void rejectsUnsafeNativeLaunchVersionIds(String versionId) {
    assertThrows(
        IllegalArgumentException.class,
        () -> MojangUtils.getModernLaunchJar(tempDir.resolve("bin"), versionId));
  }

  @Test
  void nativeAliasPreservesValidVersionIdentityUnderNormalizedBinDirectory() {
    String versionId = "neoforge-26.2.0.75+custom_patch";
    Path bin = tempDir.resolve("pack").resolve("unused").resolve("..").resolve("bin");

    assertEquals(
        tempDir.resolve("pack/bin/native-launch").resolve(versionId + ".jar").toAbsolutePath(),
        MojangUtils.getModernLaunchJar(bin, versionId));
    assertFalse(Files.exists(tempDir.resolve("pack")));
  }

  @Test
  void usesVersionDefaultUserJvmWhenUserJavaArgsAreStillDefault() {
    IMinecraftVersionInfo version =
        testVersionWithDefaultUserJvm("{\"value\":[\"-Xms2G\",\"-Xmx4G\",\"-XX:+UseZGC\"]}");

    assertEquals(
        Arrays.asList("-Xms2G", "-XX:+UseZGC"),
        MinecraftLauncher.resolveLauncherJvmArgs(
            version,
            new TestLaunchOptions(true, ""),
            new FakeJavaRuntime(),
            new StringSubstitutor(),
            4096));
  }

  @Test
  void dropsVersionInitialHeapWhenItExceedsSelectedMemory() {
    IMinecraftVersionInfo version =
        testVersionWithDefaultUserJvm("{\"value\":[\"-Xms2G\",\"-Xmx4G\",\"-XX:+UseZGC\"]}");

    assertEquals(
        Collections.singletonList("-XX:+UseZGC"),
        MinecraftLauncher.resolveLauncherJvmArgs(
            version,
            new TestLaunchOptions(true, ""),
            new FakeJavaRuntime(),
            new StringSubstitutor(),
            1024));
  }

  @Test
  void ignoresVersionDefaultUserJvmWhenUserCustomizedArgs() {
    IMinecraftVersionInfo version =
        testVersionWithDefaultUserJvm("{\"value\":[\"-Xms2G\",\"-Xmx4G\",\"-XX:+UseZGC\"]}");

    assertEquals(
        Arrays.asList("-XX:+UseSerialGC", "-Dfoo=bar"),
        MinecraftLauncher.resolveLauncherJvmArgs(
            version,
            new TestLaunchOptions(false, "-XX:+UseSerialGC -Dfoo=bar"),
            new FakeJavaRuntime(),
            new StringSubstitutor(),
            4096));
  }

  @Test
  void fallsBackToTechnicDefaultWhenVersionHasNoDefaultUserJvm() {
    IMinecraftVersionInfo version = testVersionWithDefaultUserJvm();

    assertEquals(
        Arrays.asList(TechnicSettings.DEFAULT_JAVA_ARGS.split(" ")),
        MinecraftLauncher.resolveLauncherJvmArgs(
            version,
            new TestLaunchOptions(true, ""),
            new FakeJavaRuntime(),
            new StringSubstitutor(),
            4096));
  }

  @Test
  void fallsBackToTechnicDefaultWhenChainedVersionHasNoDefaultUserJvm() {
    ChainedMinecraftVersionInfo version =
        new ChainedMinecraftVersionInfo(testVersionWithDefaultUserJvm());
    version.addVersionToChain(testVersionWithDefaultUserJvm());

    assertEquals(
        Arrays.asList(TechnicSettings.DEFAULT_JAVA_ARGS.split(" ")),
        MinecraftLauncher.resolveLauncherJvmArgs(
            version,
            new TestLaunchOptions(true, ""),
            new FakeJavaRuntime(),
            new StringSubstitutor(),
            4096));
  }

  @Test
  void resolves26_1ShapedWindowsDefaultsUsingVersionRangeRules() throws Exception {
    withOs(
        "Windows 10",
        "10.0.19045",
        () -> {
          IMinecraftVersionInfo version =
              testVersionWithDefaultUserJvm(
                  "{\"value\":[\"-Xms2G\",\"-Xmx4G\",\"-XX:+UseCompactObjectHeaders\",\"-XX:+AlwaysPreTouch\",\"-XX:+UseStringDeduplication\"]}",
                  "{\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}},{\"action\":\"allow\",\"os\":{\"name\":\"linux\"}},{\"action\":\"allow\",\"os\":{\"name\":\"windows\",\"versionRange\":{\"min\":\"10.0.17134\"}}}],\"value\":[\"-XX:+UseZGC\"]}",
                  "{\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"windows\",\"versionRange\":{\"max\":\"10.0.17134\"}}}],\"value\":[\"-XX:+UnlockExperimentalVMOptions\",\"-XX:+UseG1GC\",\"-XX:G1NewSizePercent=20\",\"-XX:G1ReservePercent=20\",\"-XX:MaxGCPauseMillis=50\",\"-XX:G1HeapRegionSize=32M\"]}");

          assertEquals(
              Arrays.asList(
                  "-Xms2G",
                  "-XX:+UseCompactObjectHeaders",
                  "-XX:+AlwaysPreTouch",
                  "-XX:+UseStringDeduplication",
                  "-XX:+UseZGC"),
              MinecraftLauncher.resolveLauncherJvmArgs(
                  version,
                  new TestLaunchOptions(true, ""),
                  new FakeJavaRuntime(),
                  new StringSubstitutor(),
                  4096));
        });
  }

  @Test
  void launcherOwnedMemoryArgumentsOnlyContainMaximumHeap() {
    assertEquals(
        Collections.singletonList("-Xmx4096m"), MinecraftLauncher.buildLauncherMemoryArgs(4096));
  }

  private ModpackModel createPack(LauncherFileSystem fileSystem) {
    ModpackModel pack =
        new ModpackModel(
            new InstalledPack(
                "offline-pack", InstalledPack.RECOMMENDED, tempDir.resolve("pack").toString()),
            null,
            null,
            fileSystem);
    pack.initDirectories();
    return pack;
  }

  private static Path writeFile(Path path) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.write(path, new byte[] {1});
  }

  private static MinecraftVersionInfo version(String json) {
    return MojangUtils.getGson().fromJson(json, MinecraftVersionInfo.class);
  }

  private static ChainedMinecraftVersionInfo nativeVersion(String versionId) {
    MinecraftVersionInfo child =
        version(
            "{\"id\":\""
                + versionId
                + "\",\"inheritsFrom\":\"1.17.1\",\"type\":\"release\","
                + "\"mainClass\":\"cpw.mods.bootstraplauncher.BootstrapLauncher\","
                + "\"libraries\":[{\"name\":\"loader:bootstrap:1\"},"
                + "{\"name\":\"loader:modules:1\",\"downloads\":{\"artifact\":{\"path\":\"declared/modules.jar\"}}},"
                + "{\"name\":\"pack:patch:1\",\"MMC-hint\":\"local\"}],"
                + "\"arguments\":{\"jvm\":[\"-Dloader.property=child\","
                + "\"-DignoreList=${version_name}.jar\","
                + "\"--module-path\",\"${library_directory}/loader/bootstrap/1/bootstrap-1.jar${classpath_separator}${library_directory}/declared/modules.jar\","
                + "\"-DlibraryDirectory=${library_directory}\"],"
                + "\"game\":[\"--version\",\"${version_name}\",\"--launchTarget\",\"child\","
                + "\"--tweakClass\",\"child.Tweaker\"]}}");
    MinecraftVersionInfo parent =
        version(
            "{\"id\":\"1.17.1\",\"type\":\"release\",\"mainClass\":\"net.minecraft.client.main.Main\","
                + "\"libraries\":[{\"name\":\"loader:bootstrap:0\"}],"
                + "\"arguments\":{\"jvm\":[\"-Dloader.property=parent\",\"-DignoreList=parent.jar\","
                + "\"-cp\",\"${classpath}\"],\"game\":[\"--version\",\"parent\","
                + "\"--launchTarget\",\"parent\",\"--tweakClass\",\"parent.Tweaker\","
                + "\"--username\",\"${auth_player_name}\",\"--accessToken\",\"${auth_access_token}\"]}}");
    ChainedMinecraftVersionInfo chained = new ChainedMinecraftVersionInfo(child);
    chained.addVersionToChain(parent);
    chained.addJvmArguments(List.of(Argument.literal("-Dloader.property=prism")));
    chained.setJavaRuntime(new FakeJavaRuntime());
    return chained;
  }

  private static List<String> buildCommands(
      LauncherFileSystem fileSystem, ModpackModel pack, IMinecraftVersionInfo version)
      throws Exception {
    MicrosoftUser offline = new MicrosoftUser("00000000000000000000000000000000", "OfflinePlayer");
    UserModel users =
        new UserModel(null, null) {
          @Override
          public MicrosoftUser getCurrentUser() {
            return offline;
          }
        };
    MinecraftLauncher launcher = new MinecraftLauncher(null, fileSystem, users, null, () -> "test");
    Method method =
        MinecraftLauncher.class.getDeclaredMethod(
            "buildCommands",
            ModpackModel.class,
            long.class,
            IMinecraftVersionInfo.class,
            LaunchOptions.class);
    method.setAccessible(true);
    try {
      @SuppressWarnings("unchecked")
      List<String> commands =
          (List<String>)
              method.invoke(
                  launcher,
                  pack,
                  4096L,
                  version,
                  new LaunchOptions(null, null, new TestLaunchOptions(false, "")));
      return commands;
    } catch (InvocationTargetException failure) {
      if (failure.getCause() instanceof Exception) {
        throw (Exception) failure.getCause();
      }
      throw failure;
    }
  }

  private static String valueAfter(List<String> commands, String option) {
    assertTrue(commands.contains(option), () -> "Missing command option " + option);
    return commands.get(commands.indexOf(option) + 1);
  }

  private static List<String> properties(List<String> commands, String prefix) {
    return commands.stream()
        .filter(argument -> argument.startsWith(prefix))
        .collect(Collectors.toList());
  }

  private static IMinecraftVersionInfo testVersionWithDefaultUserJvm(
      String... defaultUserJvmEntries) {
    StringBuilder arguments = new StringBuilder("\"game\":[],\"jvm\":[]");
    if (defaultUserJvmEntries.length > 0) {
      arguments
          .append(",\"default-user-jvm\":[")
          .append(String.join(",", defaultUserJvmEntries))
          .append("]");
    }

    return MojangUtils.getGson()
        .fromJson(
            "{"
                + "\"id\":\"26.1\","
                + "\"type\":\"release\","
                + "\"mainClass\":\"example.Main\","
                + "\"arguments\":{"
                + arguments
                + "}"
                + "}",
            MinecraftVersionInfo.class);
  }

  private static void withOs(String osName, String osVersion, ThrowingRunnable assertion)
      throws Exception {
    String previousName = System.getProperty("os.name");
    String previousVersion = System.getProperty("os.version");
    OperatingSystem previousOperatingSystem = getCachedOperatingSystem();

    try {
      System.setProperty("os.name", osName);
      System.setProperty("os.version", osVersion);
      setCachedOperatingSystem(null);
      assertion.run();
    } finally {
      restoreProperty("os.name", previousName);
      restoreProperty("os.version", previousVersion);
      setCachedOperatingSystem(previousOperatingSystem);
    }
  }

  private static OperatingSystem getCachedOperatingSystem() throws Exception {
    return (OperatingSystem) operatingSystemField().get(null);
  }

  private static void setCachedOperatingSystem(OperatingSystem operatingSystem) throws Exception {
    operatingSystemField().set(null, operatingSystem);
  }

  private static Field operatingSystemField() throws Exception {
    Field field = OperatingSystem.class.getDeclaredField("operatingSystem");
    field.setAccessible(true);
    return field;
  }

  private static void restoreProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static final class TestLaunchOptions implements ILaunchOptions {
    private final boolean usingDefaultJavaArgs;
    private final String javaArgs;

    private TestLaunchOptions(boolean usingDefaultJavaArgs, String javaArgs) {
      this.usingDefaultJavaArgs = usingDefaultJavaArgs;
      this.javaArgs = javaArgs;
    }

    @Override
    public String getClientId() {
      return "client-id";
    }

    @Override
    public WindowType getLaunchWindowType() {
      return WindowType.DEFAULT;
    }

    @Override
    public int getCustomWidth() {
      return 0;
    }

    @Override
    public int getCustomHeight() {
      return 0;
    }

    @Override
    public boolean shouldUseStencilBuffer() {
      return true;
    }

    @Override
    public String getWrapperCommand() {
      return null;
    }

    @Override
    public String getJavaArgs() {
      return javaArgs;
    }

    @Override
    public boolean isUsingDefaultJavaArgs() {
      return usingDefaultJavaArgs;
    }

    @Override
    public boolean shouldUseMojangJava() {
      return true;
    }
  }

  private static final class FakeJavaRuntime implements IJavaRuntime {
    @Override
    public File getExecutableFile() {
      return new File("java");
    }

    @Override
    public String getVersion() {
      return "21";
    }

    @Override
    public String getVendor() {
      return "Test";
    }

    @Override
    public String getOsArch() {
      return "amd64";
    }

    @Override
    public String getBitness() {
      return "64";
    }

    @Override
    public boolean is64Bit() {
      return true;
    }

    @Override
    public boolean isValid() {
      return true;
    }
  }
}
