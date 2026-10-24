package net.technicpack.launcher.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.minecraftcore.launch.WindowType;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TechnicSettingsWindowTest {
  @TempDir Path tempDir;

  @Test
  void fitToScreenPersistsWithoutReplacingCustomDimensions() throws Exception {
    Path file = tempDir.resolve("settings.json");
    TechnicSettings settings = new TechnicSettings();
    settings.setFilePath(file.toFile());
    settings.setLaunchWindowDimensions(1024, 768);
    settings.setLaunchWindowType(WindowType.FIT_TO_SCREEN);

    TechnicSettings loaded =
        Utils.getGson().fromJson(Files.readString(file), TechnicSettings.class);

    assertEquals(WindowType.FIT_TO_SCREEN, loaded.getLaunchWindowType());
    assertEquals(settings.getCustomWidth(), loaded.getCustomWidth());
    assertEquals(settings.getCustomHeight(), loaded.getCustomHeight());
  }
}
