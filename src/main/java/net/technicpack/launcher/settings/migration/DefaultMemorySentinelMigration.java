/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2026 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.settings.migration;

import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.io.UserStore;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.utilslib.Memory;

public class DefaultMemorySentinelMigration implements IMigrator {
  @Override
  public String getMigrationVersion() {
    return "2";
  }

  @Override
  public String getMigratedVersion() {
    return "3";
  }

  @Override
  public void migrate(
      TechnicSettings settings,
      InstalledPackStore packStore,
      LauncherFileSystem fileSystem,
      UserStore users) {
    if (Memory.getMemoryFromId(settings.getMemory()).getMemoryMB()
        < Memory.DEFAULT_MEM.getMemoryMB()) {
      settings.setMemory(Memory.DEFAULT_SETTINGS_ID);
    }
  }
}
