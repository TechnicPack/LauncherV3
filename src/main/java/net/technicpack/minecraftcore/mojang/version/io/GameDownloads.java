package net.technicpack.minecraftcore.mojang.version.io;

@SuppressWarnings({"unused"})
public class GameDownloads {

	private Download client;
	private Download server;

	public Download forClient() {
		return client;
	}

	public Download forServer() {
		return server;
	}

}
