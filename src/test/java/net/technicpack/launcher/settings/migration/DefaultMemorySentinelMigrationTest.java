package net.technicpack.launcher.settings.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.utilslib.Memory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultMemorySentinelMigrationTest {
  @TempDir Path tempDir;

  @Test
  void migratePromotesValuesBelowFourGigabytesToDefaultSentinel() {
    TechnicSettings settings = createSettingsWithMemory(0);

    new DefaultMemorySentinelMigration().migrate(settings, null, null, null);

    assertEquals(Memory.DEFAULT_SETTINGS_ID, settings.getMemory());
  }

  @Test
  void migratePromotesCustomValuesBelowFourGigabytesToDefaultSentinel() {
    TechnicSettings settings = createSettingsWithMemory(5);

    new DefaultMemorySentinelMigration().migrate(settings, null, null, null);

    assertEquals(Memory.DEFAULT_SETTINGS_ID, settings.getMemory());
  }

  @Test
  void migrateLeavesFourGigabytesUnchanged() {
    TechnicSettings settings = createSettingsWithMemory(6);

    new DefaultMemorySentinelMigration().migrate(settings, null, null, null);

    assertEquals(6, settings.getMemory());
  }

  @Test
  void migrateLeavesHigherMemoryValuesUnchanged() {
    TechnicSettings settings = createSettingsWithMemory(10);

    new DefaultMemorySentinelMigration().migrate(settings, null, null, null);

    assertEquals(10, settings.getMemory());
  }

  private TechnicSettings createSettingsWithMemory(int memoryId) {
    TechnicSettings settings = new TechnicSettings();
    settings.setFilePath(new File(tempDir.resolve("settings.json").toString()));
    settings.setMemory(memoryId);
    return settings;
  }
}
