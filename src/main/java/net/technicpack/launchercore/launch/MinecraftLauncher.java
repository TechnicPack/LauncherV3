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

package net.technicpack.launchercore.launch;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.install.User;
import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.minecraft.Library;
import net.technicpack.launchercore.util.OperatingSystem;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftLauncher {
	private final int memory;
	private final InstalledPack pack;
	private final CompleteVersion version;

	public MinecraftLauncher(int memory, InstalledPack pack, CompleteVersion version) {
		this.memory = memory;
		this.pack = pack;
		this.version = version;
	}

	public MinecraftProcess launch(User user) throws IOException {
		List<String> commands = buildCommands(user);
		StringBuilder full = new StringBuilder();
		boolean first = true;

		for (String part : commands) {
			if (!first) full.append(" ");
			full.append(part);
			first = false;
		}
		System.out.println("Running " + full.toString());
		Process process = new ProcessBuilder(commands).directory(pack.getInstalledDirectory()).redirectErrorStream(true).start();
		return new MinecraftProcess(commands, process);
	}

	private List<String> buildCommands(User user) {
		List<String> commands = new ArrayList<String>();
		commands.add(OperatingSystem.getJavaDir());

		OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();

		if (operatingSystem.equals(OperatingSystem.OSX)) {
			//TODO: -Xdock:icon=<icon path>
			commands.add("-Xdock:name=" + pack.getDisplayName());
		} else if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
			// I have no idea if this helps technic or not.
			commands.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
		}
		commands.add("-Xmx" + memory + "m");
		int permSize = 128;
		if (memory >= 2048) {
			permSize = 256;
		}
		commands.add("-XX:MaxPermSize=" + permSize + "m");
		commands.add("-Djava.library.path=" + new File(pack.getBinDir(), "natives").getAbsolutePath());
		commands.add("-Dminecraft.applet.TargetDirectory=" +  pack.getInstalledDirectory().getAbsolutePath());
		commands.add("-cp");
		commands.add(buildClassPath());
		commands.add(version.getMainClass());
		commands.addAll(Arrays.asList(getMinecraftArguments(version, pack.getInstalledDirectory(), user)));

		//TODO: Add all the other less important commands
		return commands;
	}

	private String[] getMinecraftArguments(CompleteVersion version, File gameDirectory, User user) {
		Map<String, String> map = new HashMap<String, String>();
		StrSubstitutor substitutor = new StrSubstitutor(map);
		String[] split = version.getMinecraftArguments().split(" ");

		map.put("auth_username", user.getUsername());
		map.put("auth_session", user.getSessionId());
		map.put("auth_access_token", user.getAccessToken());

		map.put("auth_player_name", user.getDisplayName());
		map.put("auth_uuid", user.getProfile().getId());

		map.put("profile_name", user.getDisplayName());
		map.put("version_name", version.getId());

		map.put("game_directory", gameDirectory.getAbsolutePath());
		map.put("game_assets", Utils.getAssetsDirectory().getAbsolutePath());

		for (int i = 0; i < split.length; i++) {
			split[i] = substitutor.replace(split[i]);
		}

		return split;
	}

	private String buildClassPath() {
		StringBuilder result = new StringBuilder();
		String separator = System.getProperty("path.separator");

		// Add all the libraries to the classpath.
		for (Library library : version.getLibrariesForOS()) {
			if (library.getNatives() != null) {
				continue;
			}

			// If minecraftforge is described in the libraries, skip it
			if (library.getName().startsWith("net.minecraftforge:minecraftforge")) {
				continue;
			}

			File file = new File(Utils.getCacheDirectory(), library.getArtifactPath());
			if (!file.isFile() || !file.exists()) {
				throw new RuntimeException("Library " + library + " not found.");
			}

			if (result.length() > 1) {
				result.append(separator);
			}
			result.append(file.getAbsolutePath());
		}

		// Add the modpack.jar to the classpath, if it exists and minecraftforge is not a library already
		File modpack = new File(pack.getBinDir(), "modpack.jar");
		if (modpack.exists()) {
			if (result.length() > 1) {
				result.append(separator);
			}
			result.append(modpack.getAbsolutePath());
		}

		// Add the minecraft jar to the classpath
		File minecraft = new File(pack.getBinDir(), "minecraft.jar");
		if (!minecraft.exists()) {
			throw new RuntimeException("Minecraft not installed for this pack: " + pack);
		}
		if (result.length() > 1) {
			result.append(separator);
		}
		result.append(minecraft.getAbsolutePath());

		return result.toString();
	}
}
