package net.technicpack.launcher.ui.components;

import java.awt.Component;
import net.technicpack.ui.lang.ResourceLoader;

/** A per-installation warning only: no launcher settings are changed. */
public final class ProcessorHashWarningDialog {
  public enum Result {
    CANCEL,
    DISABLE_CHECKS
  }

  private ProcessorHashWarningDialog() {}

  public static Result show(Component parent, ResourceLoader resources, String javaExecutable)
      throws InterruptedException {
    return LauncherWarningDialog.confirm(
            parent,
            resources,
            resources.getString("launcher.processorhashwarning.title"),
            resources.getString("launcher.processorhashwarning"),
            resources.getString("launcher.processorhashwarning.disableChecks"),
            resources.getString("launcher.processorhashwarning.details", javaExecutable))
        ? Result.DISABLE_CHECKS
        : Result.CANCEL;
  }
}
