package net.technicpack.launcher.ui.components;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.technicpack.launchercore.modpacks.ModpackModel;
import org.junit.jupiter.api.Test;

class ModpackDeleteDialogTest {
  @Test
  void nullSelectionDoesNotOpenConfirmation() {
    assertFalse(ModpackDeleteDialog.confirmDelete(null, null, null));
  }

  @Test
  void uninstalledSelectionDoesNotInspectSavesOrOpenConfirmation() {
    ModpackModel modpack =
        new ModpackModel(null, null, null, null) {
          @Override
          public boolean hasWorldSaves() {
            throw new AssertionError("An uninstalled pack must not inspect saves");
          }

          @Override
          public void delete(boolean keepSaves) {
            throw new AssertionError("An uninstalled pack must not be deleted");
          }
        };

    assertFalse(ModpackDeleteDialog.confirmDelete(null, modpack, null));
  }
}
