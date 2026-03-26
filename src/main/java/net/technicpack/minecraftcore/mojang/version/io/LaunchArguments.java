package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.annotations.SerializedName;
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
}
