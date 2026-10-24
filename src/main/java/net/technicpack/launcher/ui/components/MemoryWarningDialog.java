package net.technicpack.launcher.ui.components;

import java.awt.Component;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.MemoryPressure;

/** Advisory heap warning with a global opt-out; the configured heap is never changed. */
public final class MemoryWarningDialog {
  private MemoryWarningDialog() {}

  public static boolean confirmLaunch(
      Component parent,
      ResourceLoader resources,
      TechnicSettings settings,
      long heapMb,
      long availableMb)
      throws InterruptedException {
    if (!settings.isMemoryWarningEnabled() || !MemoryPressure.shouldWarn(heapMb, availableMb)) {
      return true;
    }
    LauncherWarningDialog.Result result =
        LauncherWarningDialog.confirm(
            parent,
            resources,
            resources.getString("launcher.memorywarning.title"),
            resources.getString(
                "launcher.memorywarning",
                Long.toString(heapMb),
                Long.toString(availableMb),
                Long.toString(MemoryPressure.HEADROOM_MB)),
            resources.getString("launcher.memorywarning.continue"),
            null,
            resources.getString("launcher.memorywarning.dontShowAgain"));
    if (result == LauncherWarningDialog.Result.CONTINUE_WITH_OPTION) {
      settings.setMemoryWarningEnabled(false);
    }
    return result != LauncherWarningDialog.Result.CANCEL;
  }
}
