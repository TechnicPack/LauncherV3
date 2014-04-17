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

package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.MojangConstants;
import net.technicpack.utilslib.ZipUtils;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;

import java.io.File;
import java.io.IOException;

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
	public void runTask(InstallTasksQueue queue) throws IOException {
		super.runTask(queue);

		String url = MojangConstants.getVersionDownload(this.minecraftVersion);
		String md5 = queue.getMirrorStore().getETag(url);
		File cache = new File(cacheDirectory, "minecraft_" + this.minecraftVersion + ".jar");

        IFileVerifier verifier = null;

        if (md5 != null && !md5.isEmpty()) {
            verifier = new MD5FileVerifier(md5);
        } else {
            verifier = new ValidZipFileVerifier();
        }

		if (!cache.exists() || !verifier.isFileValid(cache)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			queue.getMirrorStore().downloadFile(url, cache.getName(), output, cache, verifier, this);
		}

		ZipUtils.copyMinecraftJar(cache, new File(this.pack.getBinDir(), "minecraft.jar"));
	}
}
