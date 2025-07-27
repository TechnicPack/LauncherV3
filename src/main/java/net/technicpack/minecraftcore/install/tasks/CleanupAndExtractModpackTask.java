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

import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.install.ModpackZipFilter;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.rest.io.Mod;
import net.technicpack.rest.io.Modpack;
import net.technicpack.utilslib.IZipFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class CleanupAndExtractModpackTask implements IInstallTask<IMinecraftVersionInfo> {
	private final ModpackModel pack;
	private final Modpack modpack;
    private final ITasksQueue<IMinecraftVersionInfo> checkModQueue;
    private final ITasksQueue<IMinecraftVersionInfo> downloadModQueue;
    private final ITasksQueue<IMinecraftVersionInfo> copyModQueue;

	public CleanupAndExtractModpackTask(ModpackModel pack, Modpack modpack, ITasksQueue<IMinecraftVersionInfo> checkModQueue, ITasksQueue<IMinecraftVersionInfo> downloadModQueue, ITasksQueue<IMinecraftVersionInfo> copyModQueue) {
		this.pack = pack;
		this.modpack = modpack;
        this.checkModQueue = checkModQueue;
        this.downloadModQueue = downloadModQueue;
        this.copyModQueue = copyModQueue;
	}

	@Override
	public String getTaskDescription() {
		return "Wiping Folders";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException {
        final File binDir = pack.getBinDir();

        //If we're installing a new version of modpack, then we need to get rid of the existing version.json
        File versionFile = new File(binDir, "version.json");
        removeFile(versionFile);

        // Remove bin/install_profile.json, which is used by ForgeWrapper to install Forge in Minecraft 1.13+
        // (and the latest few Forge builds in 1.12.2)
        File installProfileFile = new File(binDir, "install_profile.json");
        removeFile(installProfileFile);

        // Delete all other version JSON files in the bin dir
        File[] binFiles = binDir.listFiles();
        if (binFiles != null) {
            final Pattern minecraftVersionPattern = Pattern.compile("^\\d++(\\.\\d++)++\\.json$");
            for (File binFile : binFiles) {
                if (minecraftVersionPattern.matcher(binFile.getName()).matches()) {
                    removeFile(binFile);
                }
            }
        }

        // Remove the runData file between updates/reinstall as well
        File runData = new File(binDir, "runData");
        removeFile(runData);

        // Remove the bin/modpack.jar file
        // This prevents issues when upgrading a modpack between a version that has a modpack.jar, and
        // one that doesn't. One example of this is updating BareBonesPack from a Forge to a Fabric build.
        File modpackJar = new File(binDir, "modpack.jar");
        removeFile(modpackJar);

        // Clean out the mods
        deleteMods(pack.getModsDir());

        // Clean out the coremods
        deleteMods(pack.getCoremodsDir());

        File modpackInstallDirectory = pack.getInstalledDirectory();

        // Clean out Flan's mods because they're outside for some reason
        File flansDir = new File(modpackInstallDirectory, "Flan");
        deleteMods(flansDir);

        IZipFileFilter zipFilter = new ModpackZipFilter(pack);

		final File cacheDir = pack.getCacheDir();

		ArrayList<File> processedFiles = new ArrayList<>(modpack.getMods().size());

		for (Mod mod : modpack.getMods()) {
			String url = mod.getUrl();
			String md5 = mod.getMd5();

			File cacheFile = mod.generateSafeCacheFile(cacheDir);

			if (processedFiles.contains(cacheFile)) {
				throw new IOException("Detected overlapping files for modpack " + pack.getName() + ": " + cacheFile.getName());
			}

			processedFiles.add(cacheFile);

            IFileVerifier verifier;

            if (md5 != null && !md5.isEmpty())
                verifier = new MD5FileVerifier(md5);
            else
                verifier = new ValidZipFileVerifier();

            EnsureFileTask<IMinecraftVersionInfo> ensureFileTask = new EnsureFileTask<>(downloadModQueue, cacheFile)
                    .withUrl(url)
                    .withVerifier(verifier)
                    .withExtractTo(modpackInstallDirectory, copyModQueue)
                    .withZipFilter(zipFilter);

			checkModQueue.addTask(ensureFileTask);
		}

		copyModQueue.addTask(new CleanupModpackCacheTask(pack, modpack));
	}

	private void deleteMods(File modsDir) throws CacheDeleteException {
        if (modsDir == null || !modsDir.exists() || !modsDir.isDirectory()) {
            return; // Nothing to delete
        }

		for (File mod : modsDir.listFiles()) {
			if (mod.isDirectory()) {
				deleteMods(mod);
				continue;
			}

			if (mod.getName().endsWith(".zip") || mod.getName().endsWith(".jar") || mod.getName().endsWith(".litemod")) {
                removeFile(mod);
			}
		}
	}

    private void removeFile(File file) throws CacheDeleteException {
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                throw new CacheDeleteException(file.getAbsolutePath(), e);
            }
        }
    }
}
