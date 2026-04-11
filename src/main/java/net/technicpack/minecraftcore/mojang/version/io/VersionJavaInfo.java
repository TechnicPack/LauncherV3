package net.technicpack.minecraftcore.mojang.version.io;

@SuppressWarnings("unused")
public class VersionJavaInfo {
  private String component;
  private int majorVersion;

  private VersionJavaInfo() {
    // Empty constructor for GSON
  }

  public VersionJavaInfo(String component, int majorVersion) {
    this.component = component;
    this.majorVersion = majorVersion;
  }

  public String getComponent() {
    return component;
  }

  public int getMajorVersion() {
    return majorVersion;
  }
}
