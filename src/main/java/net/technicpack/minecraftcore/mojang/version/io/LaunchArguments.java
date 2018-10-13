package net.technicpack.minecraftcore.mojang.version.io;

import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

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
