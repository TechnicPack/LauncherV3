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
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.install.tasks.CleanupModpackCacheTask;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.Mod;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;

import java.io.File;
import java.io.IOException;

public class CleanupAndExtractModpackTask implements IInstallTask {
	private final ModpackModel pack;
	private final Modpack modpack;
    private final ITasksQueue checkModQueue;
    private final ITasksQueue downloadModQueue;
    private final ITasksQueue copyModQueue;

	public CleanupAndExtractModpackTask(ModpackModel pack, Modpack modpack, ITasksQueue checkModQueue, ITasksQueue downloadModQueue, ITasksQueue copyModQueue) {
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
	public void runTask(InstallTasksQueue queue) throws IOException {
		File modsDir = this.pack.getModsDir();

		if (modsDir != null && modsDir.exists()) {
			deleteMods(modsDir);
		}

		File coremodsDir = this.pack.getCoremodsDir();

		if (coremodsDir != null && coremodsDir.exists()) {
			deleteMods(coremodsDir);
		}

		//HACK - jamioflan is a big jerk who needs to put his mods in the dang mod directory!
		File flansDir = new File(this.pack.getInstalledDirectory(), "Flan");

		if (flansDir.exists()) {
			deleteMods(flansDir);
		}

		File packOutput = this.pack.getInstalledDirectory();
		for (Mod mod : modpack.getMods()) {
			String url = mod.getUrl();
			String md5 = mod.getMd5();

			String version = mod.getVersion();

			String name;
			if (version != null) {
				name = mod.getName() + "-" + version + ".zip";
			} else {
				name = mod.getName() + ".zip";
			}

			File cache = new File(this.pack.getCacheDir(), name);

            IFileVerifier verifier = null;

            if (md5 != null && !md5.isEmpty())
                verifier = new MD5FileVerifier(md5);
            else
                verifier = new ValidZipFileVerifier();

			checkModQueue.addTask(new EnsureFileTask(cache, verifier, packOutput, url, downloadModQueue, copyModQueue));
		}

		copyModQueue.addTask(new CleanupModpackCacheTask(this.pack, modpack));
	}

	private void deleteMods(File modsDir) throws CacheDeleteException {
		for (File mod : modsDir.listFiles()) {
			if (mod.isDirectory()) {
				deleteMods(mod);
				continue;
			}

			if (mod.getName().endsWith(".zip") || mod.getName().endsWith(".jar") || mod.getName().endsWith(".litemod")) {
				if (!mod.delete()) {
					throw new CacheDeleteException(mod.getAbsolutePath());
				}
			}
		}
	}
}
