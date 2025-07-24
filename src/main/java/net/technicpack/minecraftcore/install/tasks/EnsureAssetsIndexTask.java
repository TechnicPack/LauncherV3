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
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidJsonFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.AssetIndex;

import java.io.File;

public class EnsureAssetsIndexTask implements IInstallTask<IMinecraftVersionInfo> {

    private final File assetsDirectory;
    private final ModpackModel modpack;
    private final ITasksQueue<IMinecraftVersionInfo> downloadIndexQueue;
    private final ITasksQueue<IMinecraftVersionInfo> examineIndexQueue;
    private final ITasksQueue<IMinecraftVersionInfo> checkAssetsQueue;
    private final ITasksQueue<IMinecraftVersionInfo> downloadAssetsQueue;
    private final ITasksQueue<IMinecraftVersionInfo> installAssetsQueue;

    public EnsureAssetsIndexTask(File assetsDirectory, ModpackModel modpack, ITasksQueue<IMinecraftVersionInfo> downloadIndexQueue, ITasksQueue<IMinecraftVersionInfo> examineIndexQueue, ITasksQueue<IMinecraftVersionInfo> checkAssetsQueue, ITasksQueue<IMinecraftVersionInfo> downloadAssetsQueue, ITasksQueue<IMinecraftVersionInfo> installAssetsQueue) {
        this.assetsDirectory = assetsDirectory;
        this.modpack = modpack;
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
    public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) {
        IMinecraftVersionInfo version = queue.getMetadata();

        AssetIndex assetIndex = version.getAssetIndex();

        if (assetIndex == null) {
            throw new RuntimeException("No asset index detected, cannot continue");
        }

        String assetsUrl = assetIndex.getUrl();

        File assetsFile = new File(assetsDirectory + File.separator + "indexes", assetIndex.getId() + ".json");

        (new File(assetsFile.getParent())).mkdirs();

        IFileVerifier fileVerifier;

        if (assetIndex.getSha1() != null)
            fileVerifier = new SHA1FileVerifier(assetIndex.getSha1());
        else
            fileVerifier = new ValidJsonFileVerifier(MojangUtils.getGson());

        if (!assetsFile.exists() || !fileVerifier.isFileValid(assetsFile)) {
            downloadIndexQueue.addTask(new DownloadFileTask<>(assetsUrl, assetsFile, fileVerifier));
        }

        examineIndexQueue.addTask(new InstallMinecraftAssetsTask(modpack, assetsDirectory.getAbsolutePath(), assetsFile, checkAssetsQueue, downloadAssetsQueue, installAssetsQueue));
    }

}
