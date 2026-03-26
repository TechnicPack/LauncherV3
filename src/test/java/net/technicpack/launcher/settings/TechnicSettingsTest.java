package net.technicpack.launcher.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
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
}
