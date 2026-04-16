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

    String classifier = library.resolveNativeClassifierForCurrentOs();
    assertEquals(nativeClassifier, classifier);
    assertEquals(
        "org/lwjgl/lwjgl/3.2.2/lwjgl-3.2.2-" + nativeClassifier + ".jar",
        library.getArtifactPath(classifier));
    assertEquals("native", library.getArtifactSha1(classifier));
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

    String classifier = library.resolveNativeClassifierForCurrentOs();
    assertNull(classifier);
    assertEquals(
        "com/mojang/text2speech/1.11.3/text2speech-1.11.3.jar",
        library.getArtifactPath(classifier));
    assertEquals("plain", library.getArtifactSha1(classifier));
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

  @Test
  void resolveNativeClassifierPrefersArm64SpecificKeyOverGenericOsFallback() {
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-linux\":{\"sha1\":\"generic\",\"size\":1,\"url\":\"https://example/generic.jar\"},"
                    + "\"natives-linux-arm64\":{\"sha1\":\"arm64\",\"size\":1,\"url\":\"https://example/arm64.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\"linux\":\"natives-linux\",\"linux-arm64\":\"natives-linux-arm64\"}"
                    + "}",
                Library.class);

    String classifier = library.resolveNativeClassifier("linux", "aarch64");

    assertEquals("natives-linux-arm64", classifier);
    assertEquals(
        "org/lwjgl/lwjgl/3.2.2/lwjgl-3.2.2-natives-linux-arm64.jar",
        library.getArtifactPath(classifier));
    assertEquals("arm64", library.getArtifactSha1(classifier));
  }

  @Test
  void resolveNativeClassifierFallsBackToGenericOsKeyWhenArm64KeyIsMissing() {
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-linux\":{\"sha1\":\"generic\",\"size\":1,\"url\":\"https://example/generic.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\"linux\":\"natives-linux\"}"
                    + "}",
                Library.class);

    assertEquals("natives-linux", library.resolveNativeClassifier("linux", "aarch64"));
  }

  @Test
  void resolveNativeClassifierSupportsArm32Aliases() {
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-linux-arm32\":{\"sha1\":\"arm32\",\"size\":1,\"url\":\"https://example/arm32.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\"linux-arm32\":\"natives-linux-arm32\"}"
                    + "}",
                Library.class);

    assertEquals("natives-linux-arm32", library.resolveNativeClassifier("linux", "armv7l"));
    assertEquals("natives-linux-arm32", library.resolveNativeClassifier("linux", "arm32"));
  }

  @Test
  void resolveNativeClassifierUsesGenericKeyForUnknownArchitecture() {
    Library library =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"name\":\"org.lwjgl:lwjgl:3.2.2\","
                    + "\"downloads\":{"
                    + "\"artifact\":{\"sha1\":\"plain\",\"size\":1,\"url\":\"https://example/plain.jar\"},"
                    + "\"classifiers\":{"
                    + "\"natives-linux\":{\"sha1\":\"generic\",\"size\":1,\"url\":\"https://example/generic.jar\"}"
                    + "}"
                    + "},"
                    + "\"natives\":{\"linux\":\"natives-linux\"}"
                    + "}",
                Library.class);

    assertEquals("natives-linux", library.resolveNativeClassifier("linux", "amd64"));
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
