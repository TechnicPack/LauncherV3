package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused"})
public class LaunchArguments {
  private ArgumentList game;
  private ArgumentList jvm;

  @SerializedName("default-user-jvm")
  private ArgumentList defaultUserJvm;

  private LaunchArguments() {
    // Empty constructor for GSON
  }

  public static LaunchArguments fromLegacyString(String minecraftArguments) {
    LaunchArguments args = new LaunchArguments();
    args.game = ArgumentList.fromString(minecraftArguments);
    args.jvm = null;
    return args;
  }

  public ArgumentList getGameArgs() {
    return game;
  }

  @Nullable
  public ArgumentList getJvmArgs() {
    return jvm;
  }

  @Nullable
  public ArgumentList getDefaultUserJavaArguments() {
    return defaultUserJvm;
  }

  /** Creates a new LaunchArguments with additional game arguments appended. */
  public LaunchArguments withAdditionalGameArgs(List<Argument> extraArgs) {
    LaunchArguments copy = new LaunchArguments();
    copy.jvm = this.jvm;
    copy.defaultUserJvm = this.defaultUserJvm;

    ArgumentList.Builder builder = new ArgumentList.Builder();

    if (this.game != null) {
      for (Argument arg : this.game.getArguments()) {
        builder.addArgument(arg);
      }
    }

    for (Argument arg : extraArgs) {
      builder.addArgument(arg);
    }

    copy.game = builder.build();
    return copy;
  }

  /**
   * Creates a new LaunchArguments with additional JVM arguments appended. If jvm args were null
   * (legacy format), creates a new list from the extra args.
   */
  public LaunchArguments withAdditionalJvmArgs(List<Argument> extraArgs) {
    LaunchArguments copy = new LaunchArguments();
    copy.game = this.game;
    copy.defaultUserJvm = this.defaultUserJvm;

    ArgumentList.Builder builder = new ArgumentList.Builder();

    if (this.jvm != null) {
      for (Argument arg : this.jvm.getArguments()) {
        builder.addArgument(arg);
      }
    }

    for (Argument arg : extraArgs) {
      builder.addArgument(arg);
    }

    copy.jvm = builder.build();
    return copy;
  }
}
