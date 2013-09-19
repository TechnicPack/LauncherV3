/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InstalledPacks {
	private final Map<String, InstalledPack> installedPacks = new HashMap<String, InstalledPack>();
	private final List<String> byIndex = new ArrayList<String>();
	private String selected = null;
	private int selectedIndex = 0;

	public static InstalledPacks load() {
		File settings = new File(Utils.getSettingsDirectory(), "installedPacks.json");
		if (!settings.exists()) {
			Utils.getLogger().log(Level.WARNING, "Unable to load settings from " + settings + " because it does not exist.");
			return null;
		}

		try {
			String json = FileUtils.readFileToString(settings, Charset.forName("UTF-8"));
			return Utils.getGson().fromJson(json, InstalledPacks.class);
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to load settings from " + settings, e);
			return null;
		}
	}

	public Map<String, InstalledPack> getInstalledPacks() {
		return installedPacks;
	}

	public Collection<InstalledPack> getPacks() {
		return installedPacks.values();
	}

	public InstalledPack get(int index) {
		if (index >= byIndex.size()) {
			return this.get(index - byIndex.size());
		} else if (index < 0) {
			return this.get(byIndex.size() + index);
		}

		return get(byIndex.get(index));
	}

	public int getIndex() {
		return selectedIndex;
	}

	public void reorder(int index, String pack) {
		if (byIndex.remove(pack)) {
			byIndex.add(index, pack);
		}
	}

	public int getPackIndex(String name) {
		return byIndex.indexOf(name);
	}

	public InstalledPack getSelected() {
		if (selected == null) {
			select(0);
		}

		return get(selected);
	}

	public InstalledPack select(int index) {
		return select(byIndex.get(index));
	}

	public InstalledPack select(String name) {
		InstalledPack pack = get(name);
		if (pack != null) {
			selected = name;
			this.selectedIndex = byIndex.indexOf(name);
		}

		save();
		return pack;
	}

	public void save() {
		File settings = new File(Utils.getSettingsDirectory(), "installedPacks.json");
		String json = Utils.getGson().toJson(this);

		try {
			FileUtils.writeStringToFile(settings, json, Charset.forName("UTF-8"));
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to save settings " + settings, e);
		}
	}

	public InstalledPack get(String name) {
		return installedPacks.get(name);
	}

	public InstalledPack addNew(InstalledPack installedPack) {
		InstalledPack pack = installedPacks.put(installedPack.getName(), installedPack);
		if (pack == null) {
			int loc = byIndex.size() - 1;
			byIndex.add(loc, installedPack.getName());
			select(loc);
		}
		return pack;
	}

	public InstalledPack add(InstalledPack installedPack) {
		return this.put(installedPack.getName(), installedPack);
	}

	private InstalledPack put(String key, InstalledPack value) {
		InstalledPack pack = installedPacks.put(key, value);
		if (pack == null) {
			byIndex.add(key);
		}
		save();
		return pack;
	}

	public InstalledPack getNext(int offset) {
		return get(selectedIndex + offset);
	}

	public InstalledPack getPrevious(int offset) {
		return get(selectedIndex - offset);
	}

	public InstalledPack remove(String key) {
		InstalledPack pack = installedPacks.remove(key);
		if (pack != null) {
			byIndex.remove(key);
		}
		save();
		return pack;
	}

	@Override
	public String toString() {
		return "InstalledPacks{" +
				"installedPacks=" + installedPacks +
				", byIndex=" + byIndex +
				", selected='" + selected + '\'' +
				", selectedIndex=" + selectedIndex +
				'}';
	}
}
