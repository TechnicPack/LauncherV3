package net.technicpack.minecraftcore.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.chain.ChainedMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.utilslib.OperatingSystem;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Test;

class MinecraftLauncherTest {
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
