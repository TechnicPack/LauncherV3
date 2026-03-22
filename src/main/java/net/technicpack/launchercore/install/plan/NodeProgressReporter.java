package net.technicpack.launchercore.install.plan;

import net.technicpack.launchercore.progress.CurrentItemMode;

public interface NodeProgressReporter {
  void updateNodeProgress(float percent);

  void updateCurrentItem(String label, CurrentItemMode mode, Float percent);
}
