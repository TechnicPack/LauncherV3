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
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.GameDownloads;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class InstallMinecraftIfNecessaryTask extends ListenerTask {

	private ModpackModel pack;
	private String minecraftVersion;
	private File cacheDirectory;

	public InstallMinecraftIfNecessaryTask(ModpackModel pack, String minecraftVersion, File cacheDirectory) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
		this.cacheDirectory = cacheDirectory;
	}

	@Override
	public String getTaskDescription() {
		return "Installing Minecraft";
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
		super.runTask(queue);

		MojangVersion version = ((InstallTasksQueue<MojangVersion>)queue).getMetadata();

		String url;
		GameDownloads dls = version.getDownloads();

		IFileVerifier verifier = null;

		if (dls != null) {
			url = dls.forClient().getUrl(); // TODO maybe use the sha1 sum?
			verifier = new SHA1FileVerifier(dls.forClient().getSha1());
		} else {
			url = MojangUtils.getOldVersionDownload(this.minecraftVersion);
			Utils.getLogger().log(Level.SEVERE, "Using legacy Minecraft download! Version id = " + version.getId() + "; parent = " + version.getParentVersion());

			String md5 = Utils.getETag(url);

			if (md5 != null && !md5.isEmpty()) {
				verifier = new MD5FileVerifier(md5);
			} else {
				verifier = new ValidZipFileVerifier();
			}
		}

		File cache = new File(cacheDirectory, "minecraft_" + this.minecraftVersion + ".jar");

		if (!cache.exists() || !verifier.isFileValid(cache)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			Utils.downloadFile(url, cache.getName(), output, cache, verifier, this);
		}

		MojangUtils.copyMinecraftJar(cache, new File(this.pack.getBinDir(), "minecraft.jar"));
	}

}
