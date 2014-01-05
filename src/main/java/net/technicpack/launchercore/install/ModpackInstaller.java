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

import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.HandleVersionFileTask;
import net.technicpack.launchercore.install.tasks.InitPackDirectoryTask;
import net.technicpack.launchercore.install.tasks.InstallMinecraftIfNecessaryTask;
import net.technicpack.launchercore.install.tasks.InstallModpackTask;
import net.technicpack.launchercore.install.tasks.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.VerifyVersionFilePresentTask;
import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.restful.Modpack;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.PlatformConstants;
import net.technicpack.launchercore.util.*;

import org.apache.commons.io.FileUtils;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ModpackInstaller {
	private final DownloadListener listener;
	private final InstalledPack installedPack;
	private String build;
	private boolean finished = false;

	public ModpackInstaller(DownloadListener listener, InstalledPack installedPack, String build) {
		this.listener = listener;
		this.installedPack = installedPack;
		this.build = build;
	}

	public CompleteVersion installPack(Component component) throws IOException {
		InstallTasksQueue queue = new InstallTasksQueue(this.listener, component, this.installedPack, this.build);
		queue.AddTask(new InitPackDirectoryTask(this.installedPack));

		PackInfo packInfo = this.installedPack.getInfo();
		Modpack modpack = packInfo.getModpack(this.build);
		String minecraft = modpack.getMinecraft();

		if (minecraft.startsWith("1.5")) {
			queue.AddTask(new EnsureFileTask(new File(Utils.getCacheDirectory(), "fml_libs15.zip"),new File(installedPack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs15.zip"));
		} else if (minecraft.startsWith("1.4")) {
			queue.AddTask(new EnsureFileTask(new File(Utils.getCacheDirectory(), "fml_libs.zip"),new File(installedPack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs.zip"));
		}

		Version installedVersion = this.getInstalledVersion();

		boolean shouldUpdate = installedVersion == null;
		if (!shouldUpdate && !this.build.equals(installedVersion.getVersion())) {
			int result = JOptionPane.showConfirmDialog(component, "Would you like to update this pack?", "Update Found", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				shouldUpdate = true;
			} else {
				build = installedVersion.getVersion();
			}
		}

		queue.AddTask(new VerifyVersionFilePresentTask(installedPack, minecraft));
		queue.AddTask(new HandleVersionFileTask(installedPack));

		if (shouldUpdate) {
			//If we're installing a new version of modpack, then we need to get rid of the existing version.json
			File versionFile = new File(installedPack.getBinDir(), "version.json");
			if (versionFile.exists()) {
				if (!versionFile.delete()) {
					throw new CacheDeleteException(versionFile.getAbsolutePath());
				}
			}

			queue.AddTask(new InstallModpackTask(this.installedPack, this.build));
		}

		if ((installedVersion != null && installedVersion.isLegacy()) || shouldUpdate)
			queue.AddTask(new InstallMinecraftIfNecessaryTask(this.installedPack, minecraft));

		queue.RunAllTasks();

		Version versionFile = new Version(build, false);
		versionFile.save(installedPack.getBinDir());

		finished = true;
		return queue.getCompleteVersion();
	}

	private Version getInstalledVersion() {
		Version version = null;
		File versionFile = new File(this.installedPack.getBinDir(), "version");
		if (versionFile.exists()) {
			version = Version.load(versionFile);
		} else {
			Utils.pingHttpURL(PlatformConstants.getDownloadCountUrl(this.installedPack.getName()));
			Utils.sendTracking("installModpack", this.installedPack.getName(), this.installedPack.getBuild());
		}
		return version;
	}

	public boolean isFinished() {
		return finished;
	}

	public CompleteVersion prepareOfflinePack() throws IOException {
		installedPack.getInstalledDirectory();
		installedPack.initDirectories();

		File versionFile = new File(installedPack.getBinDir(), "version.json");
		File modpackJar = new File(installedPack.getBinDir(), "modpack.jar");

		boolean didExtract = false;

		if (modpackJar.exists()) {
			didExtract = ZipUtils.extractFile(modpackJar, installedPack.getBinDir(), "version.json");
		}

		if (!versionFile.exists()) {
			throw new PackNotAvailableOfflineException(installedPack.getDisplayName());
		}

		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		return Utils.getMojangGson().fromJson(json, CompleteVersion.class);
	}
}
