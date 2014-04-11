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
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.minecraftcore.mojang.CompleteVersion;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.launchercore.util.*;

import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.utilslib.Utils;
import net.technicpack.utilslib.ZipUtils;
import org.apache.commons.io.FileUtils;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ModpackInstaller {
    private final LauncherDirectories directories;
    private final IPlatformApi platformApi;
    private final String clientId;
	private boolean finished = false;
    private MirrorStore mirrorStore;

	public ModpackInstaller(LauncherDirectories directories, MirrorStore mirrorStore, IPlatformApi platformApi, String clientId) {
		this.clientId = clientId;
        this.directories = directories;
        this.mirrorStore = mirrorStore;
        this.platformApi = platformApi;
	}

	public CompleteVersion installPack(ModpackModel modpack, String build, DownloadListener listener) throws IOException {
        modpack.save();

        InstallTasksQueue queue = new InstallTasksQueue(listener, mirrorStore);
		queue.AddTask(new InitPackDirectoryTask(modpack));

		PackInfo packInfo = modpack.getPackInfo();
		Modpack modpackData = packInfo.getModpack(build);
		String minecraft = modpackData.getMinecraft();

		if (minecraft.startsWith("1.5")) {
			queue.AddTask(new EnsureFileTask(new File(directories.getCacheDirectory(), "fml_libs15.zip"), new ValidZipFileVerifier(), new File(modpack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs15.zip"));
		} else if (minecraft.startsWith("1.4")) {
			queue.AddTask(new EnsureFileTask(new File(directories.getCacheDirectory(), "fml_libs.zip"), new ValidZipFileVerifier(), new File(modpack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs.zip"));
		}

		queue.RunAllTasks();

		Version installedVersion = modpack.getInstalledVersion();
        boolean shouldUpdate = false;

        if (installedVersion == null) {
            platformApi.incrementPackInstalls(modpack.getName());
            Utils.sendTracking("installModpack", modpack.getName(), modpack.getBuild(), clientId);
            shouldUpdate = true;
        }

//		if (!shouldUpdate && !build.equals(installedVersion.getVersion())) {
//			int result = JOptionPane.showConfirmDialog(parentComponent, "Would you like to update this pack?", "Update Found", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
//
//			if (result == JOptionPane.YES_OPTION) {
//				shouldUpdate = true;
//			} else {
//				build = installedVersion.getVersion();
//			}
//		}

		if (shouldUpdate) {
			//If we're installing a new version of modpack, then we need to get rid of the existing version.json
			File versionFile = new File(modpack.getBinDir(), "version.json");
			if (versionFile.exists()) {
				if (!versionFile.delete()) {
					throw new CacheDeleteException(versionFile.getAbsolutePath());
				}
			}

			queue.AddTask(new InstallModpackTask(modpack, modpackData));
		}

		queue.AddTask(new VerifyVersionFilePresentTask	(modpack, minecraft));
	    queue.AddTask(new HandleVersionFileTask(modpack, directories));

	    if ((installedVersion != null && installedVersion.isLegacy()) || shouldUpdate)
			queue.AddTask(new InstallMinecraftIfNecessaryTask(modpack, minecraft, directories.getCacheDirectory()));

        queue.RunAllTasks();

        Version versionFile = new Version(build, false);
        versionFile.save(modpack.getBinDir());

        finished = true;
        return queue.getCompleteVersion();
    }

	public boolean isFinished() {
		return finished;
	}

	public CompleteVersion prepareOfflinePack(ModpackModel modpack) throws IOException {
        modpack.initDirectories();

		File versionFile = new File(modpack.getBinDir(), "version.json");
		File modpackJar = new File(modpack.getBinDir(), "modpack.jar");

		boolean didExtract = false;

		if (modpackJar.exists()) {
			didExtract = ZipUtils.extractFile(modpackJar, modpack.getBinDir(), "version.json");
		}

		if (!versionFile.exists()) {
			throw new PackNotAvailableOfflineException(modpack.getDisplayName());
		}

		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		return Utils.getMojangGson().fromJson(json, CompleteVersion.class);
	}
}
