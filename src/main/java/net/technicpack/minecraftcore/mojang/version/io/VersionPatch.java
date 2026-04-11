package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a version patch file from the patches/ directory. Follows the Prism Launcher
 * VersionFile format (subset).
 */
@SuppressWarnings("unused")
public class VersionPatch {
  private int formatVersion;
  private String uid;
  private String name;
  private String version;
  private int order;

  private List<Library> libraries;

  @SerializedName("+jvmArgs")
  private List<String> jvmArgs;

  @SerializedName("+tweakers")
  private List<String> tweakers;

  private String mainClass;
  private VersionJavaInfo javaVersion;
  private List<Integer> compatibleJavaMajors;
  private List<Conflict> conflicts;

  public static class Conflict {
    private String uid;

    public String getUid() {
      return uid;
    }
  }

  public int getFormatVersion() {
    return formatVersion;
  }

  public String getUid() {
    return uid;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public int getOrder() {
    return order;
  }

  public List<Library> getLibraries() {
    return libraries;
  }

  public List<String> getJvmArgs() {
    return jvmArgs;
  }

  public List<String> getTweakers() {
    return tweakers;
  }

  public String getMainClass() {
    return mainClass;
  }

  public VersionJavaInfo getJavaVersion() {
    return javaVersion;
  }

  public List<Integer> getCompatibleJavaMajors() {
    return compatibleJavaMajors;
  }

  public List<Conflict> getConflicts() {
    return conflicts;
  }
}
