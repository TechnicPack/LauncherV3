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

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.DownloadFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidJsonFileVerifier;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;

import java.io.File;
import java.io.IOException;

public class EnsureAssetsIndexTask implements IInstallTask {
    private final File assetsDirectory;
    private final ITasksQueue downloadIndexQueue;
    private final ITasksQueue examineIndexQueue;
    private final ITasksQueue checkAssetsQueue;
    private final ITasksQueue downloadAssetsQueue;
    private final ITasksQueue installAssetsQueue;

	public EnsureAssetsIndexTask(File assetsDirectory, ITasksQueue downloadIndexQueue, ITasksQueue examineIndexQueue, ITasksQueue checkAssetsQueue, ITasksQueue downloadAssetsQueue, ITasksQueue installAssetsQueue) {
        this.assetsDirectory = assetsDirectory;
        this.downloadIndexQueue = downloadIndexQueue;
        this.examineIndexQueue = examineIndexQueue;
        this.checkAssetsQueue = checkAssetsQueue;
        this.downloadAssetsQueue = downloadAssetsQueue;
        this.installAssetsQueue = installAssetsQueue;
	}

	@Override
	public String getTaskDescription() {
		return "Retrieving assets index";
	}

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		String assets = ((InstallTasksQueue<MojangVersion>)queue).getMetadata().getAssetsKey();

		if (assets == null || assets.isEmpty()) {
			assets = "legacy";
		}

		File output = new File(assetsDirectory + File.separator + "indexes", assets+".json");

		(new File(output.getParent())).mkdirs();

        IFileVerifier fileVerifier = new ValidJsonFileVerifier(MojangUtils.getGson());
        String assetsUrl = MojangUtils.getAssetsIndex(assets);

		if (!output.exists() || !fileVerifier.isFileValid(output)) {
            downloadIndexQueue.addTask(new DownloadFileTask(assetsUrl, output, fileVerifier));
		}

        examineIndexQueue.addTask(new InstallMinecraftAssetsTask(assetsDirectory.getAbsolutePath(), output, checkAssetsQueue, downloadAssetsQueue, installAssetsQueue));
	}
}
