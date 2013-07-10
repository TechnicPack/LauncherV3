package net.technicpack.launcher.restful;

import java.util.List;

public class Build {
	private String minecraft;
	private String forge;
	private List<Mod> mods;

	public String getForge() {
		return forge;
	}

	public String getMinecraft() {
		return minecraft;
	}

	public List<Mod> getMods() {
		return mods;
	}
}
