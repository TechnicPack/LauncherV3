package net.technicpack.launcher.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerFrameCopyTest {
    @TempDir
    Path tempDir;

    @Test
    void standardInstallCopyExcludesRootSettingsFile() throws IOException {
        Path oldRoot = tempDir.resolve("old-root");
        Path newRoot = tempDir.resolve("new-root");
        Files.createDirectories(oldRoot.resolve("modpacks"));
        Files.write(oldRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(oldRoot.resolve("modpacks").resolve("pack.txt"), "ok".getBytes(StandardCharsets.UTF_8));

        InstallerFrame.copyInstallRoot(oldRoot.toFile(), newRoot.toFile(), false);

        assertFalse(Files.exists(newRoot.resolve("settings.json")));
        assertTrue(Files.exists(newRoot.resolve("modpacks").resolve("pack.txt")));
    }

    @Test
    void portableInstallCopyKeepsRootSettingsFile() throws IOException {
        Path oldRoot = tempDir.resolve("old-root");
        Path newRoot = tempDir.resolve("new-root");
        Files.createDirectories(oldRoot);
        Files.write(oldRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

        InstallerFrame.copyInstallRoot(oldRoot.toFile(), newRoot.toFile(), true);

        assertTrue(Files.exists(newRoot.resolve("settings.json")));
    }

    @Test
    void standardInstallCleanupKeepsRootSettingsFileWhenOldRootIsDefault() throws IOException {
        Path oldRoot = tempDir.resolve("old-root");
        Path oldRootSubdir = oldRoot.resolve("modpacks");
        Files.createDirectories(oldRootSubdir);
        Files.write(oldRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.write(oldRootSubdir.resolve("pack.txt"), "ok".getBytes(StandardCharsets.UTF_8));

        boolean keepSettingsFile = InstallerFrame.shouldKeepSettingsFileOnStandardCleanup(oldRoot.toFile(), oldRoot.toFile());
        InstallerFrame.cleanupInstallRoot(oldRoot.toFile(), keepSettingsFile);

        assertTrue(Files.exists(oldRoot.resolve("settings.json")));
        assertFalse(Files.exists(oldRootSubdir));
    }

    @Test
    void standardInstallCleanupDeletesRootSettingsFileWhenOldRootIsNotDefault() throws IOException {
        Path oldRoot = tempDir.resolve("old-root");
        Path defaultRoot = tempDir.resolve("default-root");
        Files.createDirectories(oldRoot);
        Files.write(oldRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

        boolean keepSettingsFile = InstallerFrame.shouldKeepSettingsFileOnStandardCleanup(oldRoot.toFile(), defaultRoot.toFile());
        InstallerFrame.cleanupInstallRoot(oldRoot.toFile(), keepSettingsFile);

        assertFalse(Files.exists(oldRoot));
    }

    @Test
    void portableInstallCleanupDeletesRootDirectory() throws IOException {
        Path oldRoot = tempDir.resolve("old-root");
        Files.createDirectories(oldRoot);
        Files.write(oldRoot.resolve("settings.json"), "{}".getBytes(StandardCharsets.UTF_8));

        InstallerFrame.cleanupInstallRoot(oldRoot.toFile(), false);

        assertFalse(Files.exists(oldRoot));
    }
}
