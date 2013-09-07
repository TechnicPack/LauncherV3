package net.technicpack.launchercore.restful.solder;

import net.technicpack.launchercore.restful.Modpack;

import java.util.List;

public class SolderModpack implements Modpack {
	private String minecraft;
	private List<Mod> mods;

	@Override
	public String getMinecraft() {
		return minecraft;
	}

	@Override
	public List<Mod> getMods() {
		return mods;
	}
}
