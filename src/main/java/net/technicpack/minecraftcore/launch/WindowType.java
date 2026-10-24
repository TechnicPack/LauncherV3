package net.technicpack.minecraftcore.launch;

public enum WindowType {
  DEFAULT,
  FULLSCREEN,
  CUSTOM,
  MAXIMIZED;

  /** Whether the game is launched with explicit --width/--height dimensions. */
  public boolean hasCustomResolution() {
    return this == CUSTOM || this == MAXIMIZED;
  }
}
