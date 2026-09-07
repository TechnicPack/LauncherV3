package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MavenCoordinateTest {
  @Test
  void buildsCanonicalPathsWithoutLosingVersionClassifierOrExtension() {
    MavenCoordinate coordinate =
        MavenCoordinate.parse("net.example_group:some-artifact:1.0+build_2:natives-${arch}@zip");

    assertEquals(
        "net/example_group/some-artifact/1.0+build_2/"
            + "some-artifact-1.0+build_2-natives-${arch}.zip",
        coordinate.getPath());
    assertEquals(
        "net/example_group/some-artifact/1.0+build_2/some-artifact-1.0+build_2-client.zip",
        coordinate.withClassifier("client").getPath());
    assertEquals(
        "net/example_group/some-artifact/1.0+build_2/some-artifact-1.0+build_2.zip",
        coordinate.withClassifier(null).getPath());
    assertEquals("natives-${arch}", coordinate.getClassifier());
  }

  @Test
  void exactArtifactIdentityIncludesVersionAndNormalizesDefaultExtension() {
    MavenCoordinate coordinate = MavenCoordinate.parse("net.example:artifact:1");

    assertEquals(MavenCoordinate.parse("net.example:artifact:1@jar"), coordinate);
    assertEquals("net/example/artifact/1/artifact-1.jar", coordinate.getPath());
    assertNotEquals(MavenCoordinate.parse("net.example:artifact:2"), coordinate);
    assertNotEquals(MavenCoordinate.parse("net.example:artifact:1:client"), coordinate);
    assertNotEquals(MavenCoordinate.parse("net.example:artifact:1@zip"), coordinate);
  }

  @Test
  void rejectsMalformedCoordinatesAndUnsafeComponents() {
    for (String value :
        Arrays.asList(
            null,
            "",
            "group:artifact",
            ":artifact:1",
            "group::1",
            "group:artifact:",
            "group:artifact:1:",
            "group:artifact:1@",
            "group:artifact:1:client:extra",
            "group:artifact:1@jar@zip",
            ".group:artifact:1",
            "group..name:artifact:1",
            "group.:artifact:1",
            "../group:artifact:1",
            "group:../artifact:1",
            "group:artifact:../1",
            "group:artifact:1:../client",
            "group:artifact:1@../jar",
            "group:artifact:..",
            "group:artifact:1:.",
            "group:artifact:1:.. ",
            "group:artifact:1@jar.",
            "group:artifact:1:natives\\windows",
            "group:artifact:1\n",
            "group:artifact:1:client\u0000")) {
      assertThrows(IllegalArgumentException.class, () -> MavenCoordinate.parse(value), value);
    }
    MavenCoordinate coordinate = MavenCoordinate.parse("group:artifact:1");
    assertThrows(IllegalArgumentException.class, () -> coordinate.withClassifier("../client"));
    assertThrows(IllegalArgumentException.class, () -> coordinate.withClassifier("client@zip"));
  }

  @Test
  void resolvesOnlySafeRelativePathsUnderTheNormalizedAbsoluteRoot() {
    Path root = Paths.get("repository-parent", "..", "libraries");
    String path = "net/example/artifact/1+build/artifact-1+build-natives-${arch}.jar";

    assertEquals(
        root.toAbsolutePath().normalize().resolve(path.replace("${arch}", "64")),
        MavenCoordinate.resolve(root, path.replace("${arch}", "64")));

    for (String invalid :
        Arrays.asList(
            null,
            "",
            ".",
            "..",
            "../outside.jar",
            "group/../outside.jar",
            "group/./artifact.jar",
            "group//artifact.jar",
            "group/artifact.jar/",
            "/absolute.jar",
            "C:/absolute.jar",
            "C:relative.jar",
            "\\\\server\\share\\artifact.jar",
            "//server/share/artifact.jar",
            "group\\artifact.jar",
            "group/artifact.jar:stream",
            "group/artifact\u0085.jar")) {
      assertThrows(
          IllegalArgumentException.class, () -> MavenCoordinate.resolve(root, invalid), invalid);
    }
  }
}
