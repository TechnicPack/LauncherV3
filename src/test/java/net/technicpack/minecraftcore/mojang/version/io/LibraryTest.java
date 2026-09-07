package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  @Test
  void selectedMetadataControlsThePathFilenameHashAndUrl() {
    Library library =
        libraryWithMetadata("custom/plain/renamed.jar", "custom/native-${arch}/renamed-native.jar");

    assertEquals("custom/plain/renamed.jar", library.getArtifactPath());
    assertEquals("renamed.jar", library.getArtifactFilename(null));
    assertEquals("plain-sha1", library.getArtifactSha1(null));
    assertEquals(
        "https://example.invalid/plain.jar",
        library.getDownloadCandidates(library.getArtifactPath(), null).iterator().next());

    String classifier = "natives-${arch}";
    assertEquals("custom/native-${arch}/renamed-native.jar", library.getArtifactPath(classifier));
    assertEquals("renamed-native.jar", library.getArtifactFilename(classifier));
    assertEquals("native-sha1", library.getArtifactSha1(classifier));
    assertEquals(
        "https://example.invalid/native.jar",
        library
            .getDownloadCandidates(
                library.getArtifactPath(classifier).replace("${arch}", "64"), classifier)
            .iterator()
            .next());
  }

  @Test
  void missingNativeMetadataNeverUsesPlainArtifactMetadata() {
    Library noClassifiers = libraryWithMetadata("custom/plain.jar", null);
    Library otherClassifier = libraryWithMetadata("custom/plain.jar", "custom/native.jar");
    for (Library library : Arrays.asList(noClassifiers, otherClassifier)) {
      String classifier = "natives-missing";

      assertNull(library.getArtifact(classifier));
      assertNull(library.getArtifactSha1(classifier));
      assertEquals(
          "org/lwjgl/lwjgl/3.2.2/lwjgl-3.2.2-natives-missing.jar",
          library.getArtifactPath(classifier));
      assertFalse(
          library
              .getDownloadCandidates(library.getArtifactPath(classifier), classifier)
              .contains("https://example.invalid/plain.jar"));
    }
  }

  @Test
  void selectedArtifactUrlPreservesRepositoryAndForgeMirrorPreference() {
    Library library = libraryWithMetadata("custom/plain.jar", "custom/native.jar");
    library.setUrl("https://legacy.example.invalid/");
    library
        .getArtifact("natives-${arch}")
        .setUrl("https://maven.minecraftforge.net/native-metadata.jar");
    String path = library.getArtifactPath("natives-${arch}");

    assertEquals(
        Arrays.asList(
            "https://legacy.example.invalid/" + path,
            TechnicConstants.TECHNIC_LIB_REPO + "native-metadata.jar",
            "https://maven.creeperhost.net/native-metadata.jar",
            "https://maven.minecraftforge.net/native-metadata.jar",
            "https://libraries.minecraft.net/" + path,
            "https://maven.minecraftforge.net/" + path,
            "https://mirror.technicpack.net/Technic/lib/" + path,
            "https://maven.creeperhost.net/" + path),
        new ArrayList<>(library.getDownloadCandidates(path, "natives-${arch}")));

    library
        .getArtifact("natives-${arch}")
        .setUrl("https://files.minecraft" + "forge.net/maven/native-metadata.jar");
    assertEquals(
        Arrays.asList(
            "https://legacy.example.invalid/" + path,
            TechnicConstants.TECHNIC_LIB_REPO + "native-metadata.jar",
            "https://maven.creeperhost.net/native-metadata.jar",
            "https://files.minecraft" + "forge.net/maven/native-metadata.jar",
            "https://libraries.minecraft.net/" + path,
            "https://maven.minecraftforge.net/" + path,
            "https://mirror.technicpack.net/Technic/lib/" + path,
            "https://maven.creeperhost.net/" + path),
        new ArrayList<>(library.getDownloadCandidates(path, "natives-${arch}")));
  }

  @Test
  void rejectsUnsafeSelectedMetadataPaths() {
    Library library = libraryWithMetadata("custom/plain.jar", "../outside.jar");

    assertEquals("custom/plain.jar", library.getArtifactPath());
    assertThrows(IllegalArgumentException.class, () -> library.getArtifactPath("natives-${arch}"));
    assertThrows(
        IllegalArgumentException.class, () -> libraryWithMetadata("", null).getArtifactPath());
  }

  @Test
  void localResolutionPrefersFlatLayoutThenAuthoritativeMavenPath(@TempDir Path pack)
      throws IOException {
    Library library = libraryWithMetadata("custom/declared/renamed.jar", null);
    Path maven = pack.resolve("libraries/custom/declared/renamed.jar");
    Files.createDirectories(maven.getParent());
    Files.write(maven, new byte[] {1});
    Path flat = pack.resolve("libraries/renamed.jar");
    Files.write(flat, new byte[] {2});

    assertEquals(flat, library.resolveLocalPath(pack));
    Files.delete(flat);
    assertEquals(maven, library.resolveLocalPath(pack));
    Files.delete(maven);
    assertNull(library.resolveLocalPath(pack));
  }

  @Test
  void flatLocalLookupCannotBypassInvalidDeclaredPaths(@TempDir Path pack) throws IOException {
    Path flat = pack.resolve("libraries/outside.jar");
    Files.createDirectories(flat.getParent());
    Files.write(flat, new byte[] {1});
    Library library = libraryWithMetadata("../outside.jar", null);

    assertThrows(IllegalArgumentException.class, () -> library.resolveLocalPath(pack));
  }

  @Test
  void libraryMergeIdentityStillIgnoresOnlyTheVersion() {
    Library oldVersion = new Library("net.example:artifact:1:client");
    Library newVersion = new Library("net.example:artifact:2:client@jar");

    assertEquals(oldVersion, newVersion);
    assertEquals(oldVersion.hashCode(), newVersion.hashCode());
    assertNotEquals(oldVersion.getArtifactPath(), newVersion.getArtifactPath());
    assertNotEquals(oldVersion, new Library("net.example:artifact:2"));
    assertNotEquals(oldVersion, new Library("net.example:artifact:2:client@zip"));
  }

  @Test
  void artifactIdentityIncludesItsDeclaredPath() {
    Library first = libraryWithMetadata("custom/first.jar", null);
    Library second = libraryWithMetadata("custom/second.jar", null);

    assertNotEquals(first.getArtifact(null), second.getArtifact(null));
    assertEquals(
        first.getArtifact(null), libraryWithMetadata("custom/first.jar", null).getArtifact(null));
  }

  private static Library libraryWithMetadata(String plainPath, String nativePath) {
    JsonObject definition = new JsonObject();
    definition.addProperty("name", "org.lwjgl:lwjgl:3.2.2");
    JsonObject downloads = new JsonObject();
    downloads.add("artifact", artifactMetadata(plainPath, "plain"));
    if (nativePath != null) {
      JsonObject classifiers = new JsonObject();
      classifiers.add("natives-${arch}", artifactMetadata(nativePath, "native"));
      downloads.add("classifiers", classifiers);
    }
    definition.add("downloads", downloads);
    JsonObject natives = new JsonObject();
    natives.addProperty("linux", "natives-${arch}");
    definition.add("natives", natives);
    return MojangUtils.getGson().fromJson(definition, Library.class);
  }

  private static JsonObject artifactMetadata(String path, String variant) {
    JsonObject artifact = new JsonObject();
    artifact.addProperty("path", path);
    artifact.addProperty("sha1", variant + "-sha1");
    artifact.addProperty("url", "https://example.invalid/" + variant + ".jar");
    return artifact;
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
