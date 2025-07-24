/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.GameDownloads;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;

public class InstallMinecraftIfNecessaryTask extends ListenerTask<IMinecraftVersionInfo> {

	private ModpackModel pack;
	private String minecraftVersion;
	private File cacheDirectory;
	private boolean forceRegeneration;

	public InstallMinecraftIfNecessaryTask(ModpackModel pack, String minecraftVersion, File cacheDirectory, boolean forceRegeneration) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
		this.cacheDirectory = cacheDirectory;
		this.forceRegeneration = forceRegeneration;
	}

	@Override
	public String getTaskDescription() {
		return "Installing Minecraft";
	}

	@Override
	public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException, InterruptedException {
		super.runTask(queue);

		IMinecraftVersionInfo version = queue.getMetadata();

		GameDownloads dls = version.getDownloads();

		if (dls == null) {
			throw new RuntimeException("Using legacy Minecraft download! Version id = " + version.getId() + "; parent = " + version.getParentVersion());
		}

		String url = dls.forClient().getUrl();
		IFileVerifier verifier = new SHA1FileVerifier(dls.forClient().getSha1());

		File cache = new File(cacheDirectory, "minecraft_" + this.minecraftVersion + ".jar");

		boolean regenerate = forceRegeneration;

		if (!cache.exists() || !verifier.isFileValid(cache)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			Utils.downloadFile(url, cache.getName(), output, cache, verifier, this);
			regenerate = true;
		}

		File binMinecraftJar = new File(this.pack.getBinDir(), "minecraft.jar");

		if (!binMinecraftJar.exists() || regenerate) {
			MojangUtils.copyMinecraftJar(cache, binMinecraftJar, this);
		}
	}

}
