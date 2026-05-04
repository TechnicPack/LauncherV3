package net.technicpack.autoupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelauncherTest {
  private static final int EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS = 40;
  private static final long EXPECTED_WINDOWS_PACKAGE_RETRY_DELAY_MILLIS = 500L;

  @Test
  void replaceLauncherPackageRetriesWhenWindowsTemporarilyLocksTarget() throws Exception {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();
    List<Long> pauses = new ArrayList<>();

    Relauncher.replaceLauncherPackage(
        currentPath,
        targetPath,
        true,
        (source, target) -> {
          if (attempts.incrementAndGet() < 3) {
            throw new AccessDeniedException(target.toString());
          }
        },
        pauses::add);

    assertEquals(3, attempts.get());
    assertEquals(
        List.of(
            EXPECTED_WINDOWS_PACKAGE_RETRY_DELAY_MILLIS,
            EXPECTED_WINDOWS_PACKAGE_RETRY_DELAY_MILLIS),
        pauses);
  }

  @Test
  void replaceLauncherPackageDoesNotRetryWhenFailureDoesNotTargetDestination() {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();

    FileSystemException error =
        assertThrows(
            FileSystemException.class,
            () ->
                Relauncher.replaceLauncherPackage(
                    currentPath,
                    targetPath,
                    true,
                    (source, target) -> {
                      attempts.incrementAndGet();
                      throw new FileSystemException(source.toString());
                    },
                    millis -> {}));

    assertEquals(1, attempts.get());
    assertEquals(currentPath.toString(), error.getFile());
  }

  @Test
  void replaceLauncherPackageStopsAfterConfiguredRetryLimit() {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();
    List<Long> pauses = new ArrayList<>();

    AccessDeniedException error =
        assertThrows(
            AccessDeniedException.class,
            () ->
                Relauncher.replaceLauncherPackage(
                    currentPath,
                    targetPath,
                    true,
                    (source, target) -> {
                      attempts.incrementAndGet();
                      throw new AccessDeniedException(target.toString());
                    },
                    pauses::add));

    assertEquals(targetPath.toString(), error.getFile());
    assertEquals(EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS, attempts.get());
    assertEquals(EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS - 1, pauses.size());
  }

  @Test
  void parseMoveTargetRejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> Relauncher.parseMoveTarget(null));
  }

  @Test
  void parseMoveTargetAcceptsNativePathUnchanged() {
    String native_ = "relative/path/file.exe";
    assertEquals(Paths.get(native_), Relauncher.parseMoveTarget(native_));
  }

  @Test
  void parseMoveTargetStripsLeadingSlashOnUrlFormDrivePath() {
    // Shape observed from legacy launchers: URI.getPath() output written into -movetarget.
    assertEquals(
        Paths.get("C:/Users/alice/Downloads/TechnicLauncher.exe"),
        Relauncher.parseMoveTarget("/C:/Users/alice/Downloads/TechnicLauncher.exe"));
  }

  @Test
  void parseMoveTargetPreservesSpacesAndUnicode() {
    // Some events include decoded spaces and non-ASCII characters in the URL-form path.
    assertEquals(
        Paths.get("C:/Users/bob/OneDrive/Área de Trabalho/Minecraft/TechnicLauncher.exe"),
        Relauncher.parseMoveTarget(
            "/C:/Users/bob/OneDrive/Área de Trabalho/Minecraft/TechnicLauncher.exe"));
  }

  @Test
  void parseMoveTargetDoesNotStripUnixAbsolutePath() {
    // Regression: only the "/X:" shape should be stripped. Real unix absolute paths must pass
    // through.
    String unixPath = "/home/example/.technic/launcher.jar";
    assertEquals(Paths.get(unixPath), Relauncher.parseMoveTarget(unixPath));
  }

  @Test
  void parseMoveTargetHandlesNonCDriveLetters() {
    // Events also appear on drives other than C:\ — make sure we don't hardcode the letter.
    assertEquals(
        Paths.get("Z:/games/.technic/launcher.exe"),
        Relauncher.parseMoveTarget("/Z:/games/.technic/launcher.exe"));
  }

  @Test
  void replaceLauncherPackageRetriesWhenStashedOldPathIsLocked() throws Exception {
    // The mover now renames target -> target.old before copying. If a prior run left a
    // locked target.old, the retry loop must still fire (AV commonly releases within seconds).
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    Path stashedPath = Path.of("TechnicLauncher.exe.old");
    AtomicInteger attempts = new AtomicInteger();
    List<Long> pauses = new ArrayList<>();

    Relauncher.replaceLauncherPackage(
        currentPath,
        targetPath,
        true,
        (source, target) -> {
          if (attempts.incrementAndGet() < 3) {
            throw new AccessDeniedException(stashedPath.toString());
          }
        },
        pauses::add);

    assertEquals(3, attempts.get());
    assertEquals(
        List.of(
            EXPECTED_WINDOWS_PACKAGE_RETRY_DELAY_MILLIS,
            EXPECTED_WINDOWS_PACKAGE_RETRY_DELAY_MILLIS),
        pauses);
  }

  @Test
  void replaceLauncherPackageReplacesTargetContentsOnNonWindowsHost(@TempDir Path tmp)
      throws IOException {
    // Cross-platform smoke test for the direct-copy (non-Windows) branch of
    // replaceLauncherPackageOnce. Runs the full public entry point.
    Path source = tmp.resolve("temp");
    Path target = tmp.resolve("launcher");
    Files.writeString(source, "NEW");
    Files.writeString(target, "OLD");

    Relauncher.replaceLauncherPackage(source, target);

    assertEquals("NEW", Files.readString(target));
  }

  @Test
  void cleanupStaleOldLauncherPackagesDeletesKnownSiblings(@TempDir Path tmp) throws IOException {
    for (String name : List.of("launcher.exe.old", "temp.jar.old", "unrelated.old")) {
      Files.writeString(tmp.resolve(name), "stale");
    }

    Relauncher.cleanupStaleOldLauncherPackages(new FakeFileSystem(tmp), null);

    // Targeted names gone; unrelated .old files are preserved (scoped cleanup).
    assertFalse(Files.exists(tmp.resolve("launcher.exe.old")));
    assertFalse(Files.exists(tmp.resolve("temp.jar.old")));
    assertTrue(Files.exists(tmp.resolve("unrelated.old")));
  }

  @Test
  void cleanupStaleOldLauncherPackagesSweepsSiblingOfRunningBinary(@TempDir Path tmp)
      throws IOException {
    // Simulate the common case: launcher lives on Desktop, not under .technic.
    Path desktop = Files.createDirectories(tmp.resolve("desktop"));
    Path runningBinary = desktop.resolve("TechnicLauncher.exe");
    Files.writeString(runningBinary, "current build");
    Path stashedNextToBinary = desktop.resolve("TechnicLauncher.exe.old");
    Files.writeString(stashedNextToBinary, "previous build");

    // Unrelated file that should survive the cleanup.
    Path unrelated = desktop.resolve("notes.old");
    Files.writeString(unrelated, "users other file");

    // Canonical root has its own stash that should also be swept.
    Path root = Files.createDirectories(tmp.resolve("technic"));
    Path rootStash = root.resolve("launcher.exe.old");
    Files.writeString(rootStash, "old launcher");

    Relauncher.cleanupStaleOldLauncherPackages(new FakeFileSystem(root), runningBinary);

    assertFalse(Files.exists(stashedNextToBinary), "sibling .old of running binary not deleted");
    assertFalse(Files.exists(rootStash), "canonical .technic stash not deleted");
    assertTrue(Files.exists(runningBinary), "running binary must not be deleted");
    assertTrue(Files.exists(unrelated), "unrelated .old must not be deleted");
  }

  private static final class FakeFileSystem extends net.technicpack.launcher.io.LauncherFileSystem {
    FakeFileSystem(Path root) {
      super(root);
    }
  }
}
