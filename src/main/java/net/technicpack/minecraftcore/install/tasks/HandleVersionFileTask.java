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

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.minecraftcore.mojang.CompleteVersion;
import net.technicpack.minecraftcore.mojang.Library;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class HandleVersionFileTask implements IInstallTask {
	private final ModpackModel pack;
    private final LauncherDirectories directories;
    private final ITasksQueue checkLibraryQueue;
    private final ITasksQueue downloadLibraryQueue;
    private final ITasksQueue copyLibraryQueue;

    private String libraryName;

	public HandleVersionFileTask(ModpackModel pack, LauncherDirectories directories, ITasksQueue checkLibraryQueue, ITasksQueue downloadLibraryQueue, ITasksQueue copyLibraryQueue) {
		this.pack = pack;
        this.directories = directories;
        this.checkLibraryQueue = checkLibraryQueue;
        this.downloadLibraryQueue = downloadLibraryQueue;
        this.copyLibraryQueue = copyLibraryQueue;
	}

	@Override
	public String getTaskDescription() {
        if (libraryName == null)
		    return "Processing version.";
        else
            return "Verifying "+libraryName+".";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		File versionFile = new File(this.pack.getBinDir(), "version.json");
		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		CompleteVersion version = Utils.getMojangGson().fromJson(json, CompleteVersion.class);

		if (version == null) {
			throw new DownloadException("The version.json file was invalid.");
		}

        ValidZipFileVerifier zipVerifier = new ValidZipFileVerifier();

		for (Library library : version.getLibrariesForOS()) {
			// If minecraftforge is described in the libraries, skip it
			// HACK - Please let us get rid of this when we move to actually hosting forge,
			// or at least only do it if the users are sticking with modpack.jar
			if (library.getName().startsWith("net.minecraftforge:minecraftforge") ||
					library.getName().startsWith("net.minecraftforge:forge")) {
				continue;
			}

            String[] nameBits = library.getName().split(":",3);
            libraryName = nameBits[1]+"-"+nameBits[2]+".jar";
            queue.refreshProgress();

			String natives = null;
			File extractDirectory = null;
			if (library.getNatives() != null) {
				natives = library.getNatives().get(OperatingSystem.getOperatingSystem());

				if (natives != null) {
					extractDirectory = new File(this.pack.getBinDir(), "natives");
				}
			}

			String path = library.getArtifactPath(natives).replace("${arch}", System.getProperty("sun.arch.data.model"));

			File cache = new File(directories.getCacheDirectory(), path);
			if (cache.getParentFile() != null) {
				cache.getParentFile().mkdirs();
			}

            if (cache.exists() && zipVerifier.isFileValid(cache))
                continue;

            IFileVerifier verifier = null;
            String url = library.getDownloadUrl(path, queue.getMirrorStore()).replace("${arch}", System.getProperty("sun.arch.data.model"));
            String md5 = queue.getMirrorStore().getETag(url);
            if (md5 != null && !md5.isEmpty()) {
                verifier = new MD5FileVerifier(md5);
            } else {
                verifier = zipVerifier;
            }

			checkLibraryQueue.addTask(new EnsureFileTask(cache, verifier, extractDirectory, url, library.getExtract(), downloadLibraryQueue, copyLibraryQueue));
		}

		queue.setCompleteVersion(version);
	}
}
