package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.utilslib.CryptoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstallerArtifactStoreTest {
  @TempDir Path tempDir;

  @Test
  void validatedLegacyCopyIsIndependentAndRetainsItsSource() throws Exception {
    byte[] content = bytes("verified legacy artifact");
    Path source = write(tempDir.resolve("cache/group/artifact.jar"), content);
    Path target = tempDir.resolve("libraries/group/artifact.jar");
    SHA1FileVerifier verifier = new SHA1FileVerifier(CryptoUtils.getSHA1(source));

    try (ModernInstallerLock ignored =
        ModernInstallerLock.acquire(tempDir.resolve("cache"), () -> false)) {
      assertTrue(InstallerArtifactStore.copyIfValid(source, target, verifier));
      assertArrayEquals(content, Files.readAllBytes(target));
      Files.write(target, bytes("subsequent repository replacement"));
      assertArrayEquals(content, Files.readAllBytes(source), "migration must copy, not hardlink");
    }
  }

  @Test
  void invalidSourceDoesNotReplacePublishedArtifact() throws Exception {
    Path source = write(tempDir.resolve("cache/artifact.jar"), bytes("invalid legacy artifact"));
    byte[] published = bytes("published artifact");
    Path target = write(tempDir.resolve("libraries/artifact.jar"), published);
    SHA1FileVerifier verifier = new SHA1FileVerifier(CryptoUtils.getSHA1(target));
    Set<Path> siblings = entries(target.getParent());

    try (ModernInstallerLock ignored =
        ModernInstallerLock.acquire(tempDir.resolve("cache"), () -> false)) {
      assertFalse(InstallerArtifactStore.copyIfValid(source, target, verifier));
    }

    assertArrayEquals(published, Files.readAllBytes(target));
    assertArrayEquals(bytes("invalid legacy artifact"), Files.readAllBytes(source));
    assertEquals(siblings, entries(target.getParent()));
  }

  @Test
  void sourceChangedAfterVerificationCannotPublishAndCleansOnlyOwnedStage() throws Exception {
    Path source = write(tempDir.resolve("cache/artifact.jar"), bytes("verified legacy artifact"));
    byte[] published = bytes("previous published artifact");
    Path target = write(tempDir.resolve("libraries/artifact.jar"), published);
    Path unrelated =
        write(target.resolveSibling(".installer-unrelated.tmp"), bytes("another writer"));
    Set<Path> siblings = entries(target.getParent());
    byte[] changed = bytes("legacy source changed concurrently");
    SHA1FileVerifier verifier =
        new SHA1FileVerifier(CryptoUtils.getSHA1(source)) {
          @Override
          public boolean isFileValid(Path path) {
            boolean valid = super.isFileValid(path);
            if (path.equals(source) && valid) {
              try {
                Files.write(source, changed);
              } catch (IOException failure) {
                throw new UncheckedIOException(failure);
              }
            }
            return valid;
          }
        };

    try (ModernInstallerLock ignored =
        ModernInstallerLock.acquire(tempDir.resolve("cache"), () -> false)) {
      assertThrows(
          IOException.class, () -> InstallerArtifactStore.copyIfValid(source, target, verifier));
    }

    assertArrayEquals(published, Files.readAllBytes(target));
    assertArrayEquals(changed, Files.readAllBytes(source));
    assertArrayEquals(bytes("another writer"), Files.readAllBytes(unrelated));
    assertEquals(
        siblings, entries(target.getParent()), "failed copy must remove its own stage only");
  }

  @Test
  void interruptionBeforePublicationPreservesBothFilesAndRemovesStage() throws Exception {
    byte[] content = bytes("verified legacy artifact");
    Path source = write(tempDir.resolve("cache/artifact.jar"), content);
    byte[] published = bytes("previous published artifact");
    Path target = write(tempDir.resolve("libraries/artifact.jar"), published);
    Set<Path> siblings = entries(target.getParent());
    SHA1FileVerifier verifier =
        new SHA1FileVerifier(CryptoUtils.getSHA1(source)) {
          @Override
          public boolean isFileValid(Path path) {
            boolean valid = super.isFileValid(path);
            if (!path.equals(source)) {
              Thread.currentThread().interrupt();
            }
            return valid;
          }
        };

    try (ModernInstallerLock ignored =
        ModernInstallerLock.acquire(tempDir.resolve("cache"), () -> false)) {
      try {
        assertThrows(
            InterruptedException.class,
            () -> InstallerArtifactStore.copyIfValid(source, target, verifier));
      } finally {
        Thread.interrupted();
      }
    }

    assertArrayEquals(published, Files.readAllBytes(target));
    assertArrayEquals(content, Files.readAllBytes(source));
    assertEquals(siblings, entries(target.getParent()));
  }

  @Test
  void stagesAreIndependentAndOnlyPublishReplacesTheTarget() throws Exception {
    Path target = tempDir.resolve("libraries/new/group/artifact.jar");
    byte[] first = bytes("first complete artifact");
    byte[] replacement = bytes("replacement artifact");

    try (ModernInstallerLock ignored =
        ModernInstallerLock.acquire(tempDir.resolve("cache"), () -> false)) {
      Path staged = InstallerArtifactStore.stage(target);
      Path otherStage = InstallerArtifactStore.stage(target);
      try {
        assertNotEquals(staged, otherStage);
        assertEquals(target.getParent(), staged.getParent());
        Files.write(staged, first);
        Files.write(otherStage, replacement);
        assertFalse(Files.exists(target));
        InstallerArtifactStore.publish(staged, target);
        assertFalse(Files.exists(staged));
        assertArrayEquals(first, Files.readAllBytes(target));
        assertArrayEquals(replacement, Files.readAllBytes(otherStage));
        InstallerArtifactStore.publish(otherStage, target);
        assertFalse(Files.exists(otherStage));
        assertArrayEquals(replacement, Files.readAllBytes(target));
      } finally {
        Files.deleteIfExists(staged);
        Files.deleteIfExists(otherStage);
      }
    }
  }

  @Test
  void checkedPathsCannotNameRootOrEscapeLexically() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    assertThrows(IOException.class, () -> InstallerArtifactStore.checkedPath(root, root));
    assertThrows(
        IOException.class,
        () -> InstallerArtifactStore.checkedPath(root, root.resolve("../escape")));
    assertThrows(
        IOException.class,
        () -> InstallerArtifactStore.checkedPath(root, tempDir.resolve("root-neighbor/file")));
    assertEquals(
        root.resolve("libraries/artifact.jar"),
        InstallerArtifactStore.checkedPath(
            root, root.getFileSystem().getPath("libraries/artifact.jar")));
  }

  @Test
  void configuredRootSymlinkAndContainedChildSymlinkAreAllowed() throws IOException {
    Path realRoot = Files.createDirectory(tempDir.resolve("actual-root"));
    Path root = symlink(tempDir.resolve("configured-root"), realRoot);
    Path repository = Files.createDirectory(realRoot.resolve("repository"));
    symlink(realRoot.resolve("libraries"), repository);
    Path target = root.resolve("libraries/group/artifact.jar");
    assertEquals(target, InstallerArtifactStore.checkedPath(root, target));
  }

  @Test
  void escapingDirectoryAndFinalFileSymlinksAreRejected() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path outsideFile = write(outside.resolve("artifact.jar"), bytes("unrelated outside file"));
    Path directoryLink = symlink(root.resolve("libraries"), outside);
    Path fileLink = symlink(root.resolve("artifact.jar"), outsideFile);

    assertThrows(
        IOException.class,
        () ->
            InstallerArtifactStore.checkedPath(
                root, directoryLink.resolve("missing/artifact.jar")));
    assertThrows(IOException.class, () -> InstallerArtifactStore.checkedPath(root, fileLink));
    assertArrayEquals(bytes("unrelated outside file"), Files.readAllBytes(outsideFile));
  }

  @Test
  void danglingChildSymlinkIsNotTreatedAsAnAbsentSafeDirectory() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path link = symlink(root.resolve("libraries"), tempDir.resolve("missing-outside"));
    assertThrows(
        IOException.class,
        () -> InstallerArtifactStore.checkedPath(root, link.resolve("group/artifact.jar")));
  }

  private static Path write(Path target, byte[] content) throws IOException {
    Files.createDirectories(target.getParent());
    return Files.write(target, content);
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static Set<Path> entries(Path directory) throws IOException {
    try (Stream<Path> children = Files.list(directory)) {
      return children.collect(Collectors.toSet());
    }
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
