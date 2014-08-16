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

package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.DownloadFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.TechnicConstants;
import net.technicpack.utilslib.ZipUtils;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidJsonFileVerifier;

import java.io.File;
import java.io.IOException;

public class VerifyVersionFilePresentTask implements IInstallTask {
	private ModpackModel pack;
	private String minecraftVersion;

	public VerifyVersionFilePresentTask(ModpackModel pack, String minecraftVersion) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
	}

	@Override
	public String getTaskDescription() {
		return "Retrieving Modpack Version";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		File versionFile = new File(this.pack.getBinDir(), "version.json");
		File modpackJar = new File(this.pack.getBinDir(), "modpack.jar");

		boolean didExtract = false;

		if (modpackJar.exists()) {
			didExtract = ZipUtils.extractFile(modpackJar, this.pack.getBinDir(), "version.json");
		}

        IFileVerifier fileVerifier = new ValidJsonFileVerifier();

		if (!versionFile.exists() || !fileVerifier.isFileValid(versionFile)) {
			if (this.pack.isLocalOnly()) {
				throw new PackNotAvailableOfflineException(this.pack.getDisplayName());
			} else {
				queue.addNextTask(new DownloadFileTask(TechnicConstants.getTechnicVersionJson(this.minecraftVersion), versionFile, fileVerifier));
			}
		}
	}
}
