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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.CopyFileTask;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.FileSizeVerifier;
import net.technicpack.minecraftcore.mojang.MojangConstants;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class InstallAssetsTask implements IInstallTask {
    private final String assetsDirectory;
    private final File assetsIndex;
    private final ITasksQueue checkAssetsQueue;
    private final ITasksQueue downloadAssetsQueue;
    private final ITasksQueue copyAssetsQueue;

    public InstallAssetsTask(String assetsDirectory, File assetsIndex, ITasksQueue checkAssetsQueue, ITasksQueue downloadAssetsQueue, ITasksQueue copyAssetsQueue) {
        this.assetsDirectory = assetsDirectory;
        this.assetsIndex = assetsIndex;
        this.checkAssetsQueue = checkAssetsQueue;
        this.downloadAssetsQueue = downloadAssetsQueue;
        this.copyAssetsQueue = copyAssetsQueue;
    }

    @Override
    public String getTaskDescription() {
        return "Checking Minecraft Assets";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        String json = FileUtils.readFileToString(assetsIndex, Charset.forName("UTF-8"));
        JsonObject obj = Utils.getMojangGson().fromJson(json, JsonObject.class);

        if (obj == null) {
            throw new DownloadException("The assets json file was invalid.");
        }

        boolean isVirtual = false;

        if (obj.get("virtual") != null)
            isVirtual = obj.get("virtual").getAsBoolean();

        queue.getCompleteVersion().setAreAssetsVirtual(isVirtual);

        JsonObject allObjects = obj.get("objects").getAsJsonObject();

        if (allObjects == null) {
            throw new DownloadException("The assets json file was invalid.");
        }

        for(Map.Entry<String, JsonElement> field : allObjects.entrySet()) {
            String friendlyName = field.getKey();
            JsonObject file = field.getValue().getAsJsonObject();
            String hash = file.get("hash").getAsString();
            long size = file.get("size").getAsLong();

            File location = new File(assetsDirectory + File.separator + "objects" + File.separator + hash.substring(0, 2) + File.separator, hash);
            String url = MojangConstants.getResourceUrl(hash);

            (new File(location.getParent())).mkdirs();

            File virtualOut =  new File(assetsDirectory + File.separator + "virtual" + File.separator + queue.getCompleteVersion().getAssetsKey() + File.separator + friendlyName);

            checkAssetsQueue.addTask(new EnsureFileTask(location, new FileSizeVerifier(size), null, url, virtualOut.getName(), downloadAssetsQueue, copyAssetsQueue));

            if (isVirtual && !virtualOut.exists()) {
                (new File(virtualOut.getParent())).mkdirs();
                copyAssetsQueue.addTask(new CopyFileTask(location, virtualOut));
            }
        }
    }
}
