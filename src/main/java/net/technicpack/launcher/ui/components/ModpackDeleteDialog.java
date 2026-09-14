/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.launcher.ui.components;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.ui.lang.ResourceLoader;

public final class ModpackDeleteDialog {

  private static final int DELETE_EVERYTHING = 0;
  private static final int KEEP_SAVES = 1;

  private ModpackDeleteDialog() {}

  /**
   * Asks the user to confirm deleting the given pack and performs the deletion. Packs with world
   * saves get a three-way choice (delete everything, keep the saves in place, cancel); packs
   * without get a plain yes/no confirmation.
   *
   * @return true if the pack was deleted
   */
  public static boolean confirmDelete(
      Component parent, ModpackModel modpack, ResourceLoader resources) {
    if (modpack == null || modpack.getInstalledPack() == null) {
      return false;
    }

    if (modpack.hasWorldSaves()) {
      Object[] options = {
        resources.getString("modpackoptions.delete.deleteeverything"),
        resources.getString("modpackoptions.delete.keepsaves"),
        resources.getString("modpackoptions.delete.cancel"),
      };
      int result =
          JOptionPane.showOptionDialog(
              parent,
              resources.getString("modpackoptions.delete.savestext"),
              resources.getString("modpackoptions.delete.confirmtitle"),
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              options,
              options[2]);

      if (result == DELETE_EVERYTHING || result == KEEP_SAVES) {
        modpack.delete(result == KEEP_SAVES);
        return true;
      }

      // Cancel button, Esc or closing the dialog (CLOSED_OPTION)
      return false;
    }

    int result =
        JOptionPane.showConfirmDialog(
            parent,
            resources.getString("modpackoptions.delete.confirmtext"),
            resources.getString("modpackoptions.delete.confirmtitle"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      modpack.delete();
      return true;
    }

    return false;
  }
}
