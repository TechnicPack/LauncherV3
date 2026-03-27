package net.technicpack.launcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherMainSettingsArchiveTest {
  @TempDir Path tempDir;

  @Test
  void archivesSettingsFromNonDefaultStandardInstallRoot() throws IOException {
    Path installRoot = tempDir.resolve("install-root");
    Path defaultRoot = tempDir.resolve("default-root");
    Files.createDirectories(installRoot);
    Files.write(installRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

    Path archivedPath =
        LauncherMain.archiveLegacyInstallSettingsFile(installRoot, defaultRoot, false);

    assertNotNull(archivedPath);
    assertTrue(Files.exists(archivedPath));
    assertFalse(Files.exists(installRoot.resolve("settings.json")));
  }

  @Test
  void doesNotArchiveWhenInstallRootIsDefault() throws IOException {
    Path installRoot = tempDir.resolve("install-root");
    Files.createDirectories(installRoot);
    Files.write(installRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

    Path archivedPath =
        LauncherMain.archiveLegacyInstallSettingsFile(installRoot, installRoot, false);

    assertNull(archivedPath);
    assertTrue(Files.exists(installRoot.resolve("settings.json")));
  }

  @Test
  void doesNotArchiveInPortableMode() throws IOException {
    Path installRoot = tempDir.resolve("install-root");
    Path defaultRoot = tempDir.resolve("default-root");
    Files.createDirectories(installRoot);
    Files.write(installRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

    Path archivedPath =
        LauncherMain.archiveLegacyInstallSettingsFile(installRoot, defaultRoot, true);

    assertNull(archivedPath);
    assertTrue(Files.exists(installRoot.resolve("settings.json")));
  }

  @Test
  void archivesWithSuffixWhenLegacyFileAlreadyExists() throws IOException {
    Path installRoot = tempDir.resolve("install-root");
    Path defaultRoot = tempDir.resolve("default-root");
    Files.createDirectories(installRoot);
    Files.write(installRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));
    Files.write(installRoot.resolve("settings.json.legacy"), "{}".getBytes(StandardCharsets.UTF_8));

    Path archivedPath =
        LauncherMain.archiveLegacyInstallSettingsFile(installRoot, defaultRoot, false);

    assertNotNull(archivedPath);
    assertTrue(archivedPath.getFileName().toString().startsWith("settings.json.legacy."));
    assertTrue(Files.exists(installRoot.resolve("settings.json.legacy")));
    assertTrue(Files.exists(archivedPath));
    assertFalse(Files.exists(installRoot.resolve("settings.json")));
  }
}
