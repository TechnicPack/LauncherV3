package net.technicpack.launchercore.restful;

import net.technicpack.launchercore.restful.solder.Mod;

import java.util.List;

public interface Modpack {

	public String getMinecraft();

	public List<Mod> getMods();
}
