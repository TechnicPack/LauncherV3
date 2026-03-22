package net.technicpack.launchercore.progress;

public interface ExecutionProgressListener {
  void overallChanged(String label, float percent);

  void currentItemChanged(String label, CurrentItemMode mode, Float percent);
}
