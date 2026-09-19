package net.technicpack.launcher.ui.components;

import java.awt.Component;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.MemoryPressure;

/** A per-launch warning only: neither the configured heap nor the settings file is changed. */
public final class MemoryWarningDialog {
  private MemoryWarningDialog() {}

  public static boolean confirmLaunch(
      Component parent, ResourceLoader resources, long heapMb, long availableMb)
      throws InterruptedException {
    if (!MemoryPressure.shouldWarn(heapMb, availableMb)) return true;
    return LauncherWarningDialog.confirm(
        parent,
        resources,
        resources.getString("launcher.memorywarning.title"),
        resources.getString(
            "launcher.memorywarning",
            Long.toString(heapMb),
            Long.toString(availableMb),
            Long.toString(MemoryPressure.HEADROOM_MB)),
        resources.getString("launcher.memorywarning.continue"),
        null);
  }
}
