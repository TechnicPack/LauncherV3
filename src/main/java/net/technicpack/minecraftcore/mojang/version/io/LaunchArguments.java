package net.technicpack.minecraftcore.mojang.version.io;

import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class LaunchArguments {

	private ArgumentList game;
	private ArgumentList jvm;

	public ArgumentList getGameArgs() {
		return game;
	}

	public ArgumentList getJvmArgs() {
		return jvm;
	}

}
