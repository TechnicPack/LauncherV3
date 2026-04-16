package net.technicpack.launcher.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.utilslib.Memory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TechnicSettingsTest {
  @TempDir Path tempDir;

  @Test
  void saveCreatesMissingParentDirectory() {
    Path settingsPath = tempDir.resolve("missing-parent").resolve("settings.json");

    TechnicSettings settings = new TechnicSettings();
    settings.setFilePath(new File(settingsPath.toString()));

    settings.save();

    assertTrue(Files.exists(settingsPath));
  }

  @Test
  void defaultsToDefaultMemorySentinel() {
    TechnicSettings settings = new TechnicSettings();

    assertEquals(Memory.DEFAULT_SETTINGS_ID, settings.getMemory());
  }

  @Test
  void defaultMemorySentinelResolvesToFourGigabytes() {
    TechnicSettings settings = new TechnicSettings();

    assertEquals(4096, Memory.getMemoryFromId(settings.getMemory()).getMemoryMB());
  }

  @Test
  void isUsingDefaultJavaArgsReturnsTrueWhenUnset() {
    TechnicSettings settings = new TechnicSettings();

    assertTrue(settings.isUsingDefaultJavaArgs());
    assertEquals(TechnicSettings.DEFAULT_JAVA_ARGS, settings.getJavaArgs());
  }

  @Test
  void isUsingDefaultJavaArgsReturnsTrueWhenSetToEmptyString() {
    TechnicSettings settings = new TechnicSettings();
    settings.setJavaArgs("");

    assertTrue(settings.isUsingDefaultJavaArgs());
    assertEquals(TechnicSettings.DEFAULT_JAVA_ARGS, settings.getJavaArgs());
  }

  @Test
  void isUsingDefaultJavaArgsReturnsTrueWhenSetToDefaultString() {
    TechnicSettings settings = new TechnicSettings();
    settings.setJavaArgs(TechnicSettings.DEFAULT_JAVA_ARGS);

    ILaunchOptions launchOptions = settings;

    assertTrue(launchOptions.isUsingDefaultJavaArgs());
    assertEquals(TechnicSettings.DEFAULT_JAVA_ARGS, launchOptions.getJavaArgs());
  }

  @Test
  void isUsingDefaultJavaArgsReturnsFalseWhenCustomArgsAreSet() {
    TechnicSettings settings = new TechnicSettings();
    settings.setJavaArgs("-Xmx2G");

    assertFalse(settings.isUsingDefaultJavaArgs());
    assertEquals("-Xmx2G", settings.getJavaArgs());
  }

  @Test
  void saveLeavesNoTempSibling() {
    Path settingsPath = tempDir.resolve("settings.json");

    TechnicSettings settings = new TechnicSettings();
    settings.setFilePath(settingsPath.toFile());
    settings.save();
    settings.save();

    assertTrue(Files.exists(settingsPath));
    assertFalse(
        Files.exists(tempDir.resolve("settings.json.tmp")),
        "save() must atomically rename its temp file, not leave it behind");
  }

  @Test
  void saveOverwritesExistingFileContentCompletely() throws IOException {
    Path settingsPath = tempDir.resolve("settings.json");

    TechnicSettings first = new TechnicSettings();
    first.setFilePath(settingsPath.toFile());
    first.setJavaArgs("-Xmx1G");
    first.save();

    TechnicSettings second = new TechnicSettings();
    second.setFilePath(settingsPath.toFile());
    second.setJavaArgs("-Xmx4G");
    second.save();

    String json = new String(Files.readAllBytes(settingsPath), StandardCharsets.UTF_8);
    assertTrue(json.contains("-Xmx4G"), "Second save should replace the first");
    assertFalse(
        json.contains("-Xmx1G"),
        "Atomic rename should fully overwrite, not append or leave stale content");
  }
}
