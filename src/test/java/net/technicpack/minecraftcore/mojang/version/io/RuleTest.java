package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Test;

class RuleTest {
  @Test
  void compareDottedVersionTreatsMissingTrailingPartsAsZero() throws Exception {
    assertEquals(0, compareDottedVersion("10.0", "10.0.0"));
  }

  @Test
  void compareDottedVersionComparesPartsNumerically() throws Exception {
    assertTrue(compareDottedVersion("10.0.9", "10.0.10") < 0);
  }

  @Test
  void compareDottedVersionHandlesLongerLeftVersionWithNonZeroTail() throws Exception {
    assertTrue(compareDottedVersion("10.0.1", "10.0.0.9") > 0);
  }

  @Test
  void compareDottedVersionHandlesLongerRightVersionWithNonZeroTail() throws Exception {
    assertTrue(compareDottedVersion("10.0.0.9", "10.0.1") < 0);
  }

  @Test
  void compareDottedVersionOrdersLaterBuildNumbersHigher() throws Exception {
    assertTrue(compareDottedVersion("10.0.22000", "10.0.19045") > 0);
  }

  @Test
  void allowRuleAppliesWhenOsVersionMeetsMinimumInclusiveBound() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"versionRange\":{\"min\":\"10.0.17134\"}"
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 10", "10.0.17134", () -> assertTrue(isAllowable(rule)));
  }

  @Test
  void allowRuleDoesNotApplyWhenOsVersionIsBelowMinimumBound() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"versionRange\":{\"min\":\"10.0.17134\"}"
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 10", "10.0.17133", () -> assertFalse(isAllowable(rule)));
  }

  @Test
  void allowRuleDoesNotApplyWhenOsVersionExceedsMaximumBound() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"versionRange\":{\"max\":\"10.0.17134\"}"
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 10", "10.0.17135", () -> assertFalse(isAllowable(rule)));
  }

  @Test
  void legacyVersionRegexStillWorks() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"version\":\"10\\\\.0\\\\..*\""
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 10", "10.0.19045", () -> assertTrue(isAllowable(rule)));
  }

  @Test
  void launcherMetaWindowsVersionRegexMatchesWindows10Builds() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"version\":\"^10\\\\.\""
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 10", "10.0.19045", () -> assertTrue(isAllowable(rule)));
  }

  @Test
  void launcherMetaWindowsVersionRegexRejectsPreWindows10Builds() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"windows\","
                    + "\"version\":\"^10\\\\.\""
                    + "}"
                    + "}",
                Rule.class);

    withOs("Windows 8.1", "6.3.9600", () -> assertFalse(isAllowable(rule)));
  }

  @Test
  void launcherMetaLegacyMacRegexMatchesSnowLeopardBuilds() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"osx\","
                    + "\"version\":\"^10\\\\.5\\\\.\\\\d$\""
                    + "}"
                    + "}",
                Rule.class);

    withOs("Mac OS X", "10.5.8", () -> assertTrue(isAllowable(rule)));
  }

  @Test
  void launcherMetaLegacyMacRegexRejectsNonSnowLeopardBuilds() throws Exception {
    Rule rule =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"action\":\"allow\","
                    + "\"os\":{"
                    + "\"name\":\"osx\","
                    + "\"version\":\"^10\\\\.5\\\\.\\\\d$\""
                    + "}"
                    + "}",
                Rule.class);

    withOs("Mac OS X", "10.6.8", () -> assertFalse(isAllowable(rule)));
  }

  private static boolean isAllowable(Rule rule) {
    return Rule.isAllowable(Collections.singletonList(rule), null, new FakeJavaRuntime());
  }

  private static int compareDottedVersion(String left, String right) throws Exception {
    Method method =
        osVersionRangeClass().getDeclaredMethod("compareDottedVersion", String.class, String.class);
    method.setAccessible(true);
    return (int) method.invoke(null, left, right);
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

  private static void restoreProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
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

  private static Class<?> osVersionRangeClass() throws Exception {
    return Class.forName(Rule.class.getName() + "$OsVersionRange");
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
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
