package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Test;

class LibraryTest {
  @Test
  void installResolutionUsesNativeClassifierWhenCurrentOsMappingExists() {
    String nativeClassifier = "natives-current";
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\""
                    + nativeClassifier
                    + "\":{\"sha1\":\"native\",\"size\":1,\"url\":\"https://example/native.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\""
                    + currentOsKey()
                    + "\":\""
                    + nativeClassifier
                    + "\"}"
                    + "}",
                Library.class);

    assertEquals(nativeClassifier, library.resolveNativeClassifierForCurrentOs());
    assertEquals(
        "org/lwjgl/lwjgl/3.2.2/lwjgl-3.2.2-" + nativeClassifier + ".jar",
        library.getInstallArtifactPathForCurrentOs());
    assertEquals("native", library.getInstallArtifactSha1ForCurrentOs());
    assertTrue(library.shouldExtractToNativesDirectory());
  }

  @Test
  void installResolutionFallsBackToPlainArtifactWhenNativeEntryHasNoCurrentOsClassifier() {
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"com.mojang:text2speech:1.11.3\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-other\":{\"sha1\":\"native\",\"size\":1,\"url\":\"https://example/native.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\""
                    + otherOsKey()
                    + "\":\"natives-other\"}"
                    + "}",
                Library.class);

    assertNull(library.resolveNativeClassifierForCurrentOs());
    assertEquals(
        "com/mojang/text2speech/1.11.3/text2speech-1.11.3.jar",
        library.getInstallArtifactPathForCurrentOs());
    assertEquals("plain", library.getInstallArtifactSha1ForCurrentOs());
    assertFalse(library.shouldExtractToNativesDirectory());
  }

  @Test
  void classpathEligibilityExcludesNativeEntries() {
    Library nativeLibrary =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"natives\":{\""
                    + currentOsKey()
                    + "\":\"natives-current\"}"
                    + "}",
                Library.class);
    Library plainLibrary = new Library("org.lwjgl:lwjgl:3.2.2");

    assertFalse(nativeLibrary.shouldAppearOnClasspath());
    assertTrue(plainLibrary.shouldAppearOnClasspath());
  }

  private static String currentOsKey() {
    return OperatingSystem.getOperatingSystem().getName();
  }

  private static String otherOsKey() {
    for (OperatingSystem operatingSystem : OperatingSystem.values()) {
      if (operatingSystem == OperatingSystem.UNKNOWN) {
        continue;
      }
      if (operatingSystem != OperatingSystem.getOperatingSystem()) {
        return operatingSystem.getName();
      }
    }

    return OperatingSystem.LINUX.getName();
  }
}
