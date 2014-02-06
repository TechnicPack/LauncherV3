package net.technicpack.launchercore.install;

import java.util.List;
import java.util.Map;

public interface IPackStore {
	Map<String, InstalledPack> getInstalledPacks();
	List<String> getPackNames();
	int getSelectedIndex();
	void setSelectedIndex(int index);
	void reorder(int index, String pack);
	InstalledPack add(InstalledPack installedPack);
	InstalledPack put(InstalledPack installedPack);
	InstalledPack remove(String name);
	void save();
}
