package net.technicpack.launcher.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicSettingsTest {
    @TempDir
    Path tempDir;

    @Test
    void saveCreatesMissingParentDirectory() {
        Path settingsPath = tempDir.resolve("missing-parent").resolve("settings.json");

        TechnicSettings settings = new TechnicSettings();
        settings.setFilePath(new File(settingsPath.toString()));

        settings.save();

        assertTrue(Files.exists(settingsPath));
    }
}
