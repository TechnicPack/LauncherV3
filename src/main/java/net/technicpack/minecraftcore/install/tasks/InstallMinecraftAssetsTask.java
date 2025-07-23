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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.CopyFileTask;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.FileSizeVerifier;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class InstallMinecraftAssetsTask implements IInstallTask<MojangVersion> {
    private final ModpackModel modpack;
    private final String assetsDirectory;
    private final Path assetsIndex;
    private final ITasksQueue<MojangVersion> checkAssetsQueue;
    private final ITasksQueue<MojangVersion> downloadAssetsQueue;
    private final ITasksQueue<MojangVersion> copyAssetsQueue;

    private static final String VIRTUAL_FIELD = "virtual";
    private static final String MAP_TO_RESOURCES_FIELD = "map_to_resources";
    private static final String OBJECTS_FIELD = "objects";
    private static final String SIZE_FIELD = "size";
    private static final String HASH_FIELD = "hash";

    public InstallMinecraftAssetsTask(ModpackModel modpack, String assetsDirectory, File assetsIndex, ITasksQueue<MojangVersion> checkAssetsQueue, ITasksQueue<MojangVersion> downloadAssetsQueue, ITasksQueue<MojangVersion> copyAssetsQueue) {
        this.modpack = modpack;
        this.assetsDirectory = assetsDirectory;
        this.assetsIndex = assetsIndex.toPath();
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
    public void runTask(InstallTasksQueue<MojangVersion> queue) throws IOException {
        JsonObject obj = readAssetsIndex();

        boolean isVirtual = false;

        if (obj.has(VIRTUAL_FIELD) && obj.get(VIRTUAL_FIELD).isJsonPrimitive()) {
            isVirtual = obj.get(VIRTUAL_FIELD).getAsBoolean();
        }

        boolean mapToResources = false;

        if (obj.has(MAP_TO_RESOURCES_FIELD) && obj.get(MAP_TO_RESOURCES_FIELD).isJsonPrimitive()) {
            mapToResources = obj.get(MAP_TO_RESOURCES_FIELD).getAsBoolean();
        }

        MojangVersion version = queue.getMetadata();

        version.setAreAssetsVirtual(isVirtual);
        version.setAssetsMapToResources(mapToResources);

        JsonObject allAssets = obj.get(OBJECTS_FIELD).getAsJsonObject();

        if (allAssets == null) {
            throw new DownloadException("The assets json file was invalid.");
        }

        String assetsKey = version.getAssetsKey();
        if (assetsKey == null || assetsKey.isEmpty()) {
            assetsKey = "legacy";
        }

        for (Map.Entry<String, JsonElement> assetObj : allAssets.entrySet()) {
            processAsset(assetObj, assetsKey, isVirtual, mapToResources);
        }
    }

    private JsonObject readAssetsIndex() throws IOException {
        try (Reader reader = Files.newBufferedReader(assetsIndex, StandardCharsets.UTF_8)) {
            JsonObject jsonObject = MojangUtils.getGson().fromJson(reader, JsonObject.class);

            if (jsonObject == null) {
                throw new DownloadException(String.format("The assets file %s is invalid", assetsIndex));
            }

            return jsonObject;
        } catch (JsonParseException ex) {
            throw new IOException(String.format("Failed to load assets index file %s", assetsIndex), ex);
        }
    }

    private void processAsset(Map.Entry<String, JsonElement> assetObj, String assetsKey, boolean isVirtual, boolean mapToResources) throws IOException {
        String assetPath = assetObj.getKey();
        JsonObject assetData = assetObj.getValue().getAsJsonObject();

        String hash = assetData.get(HASH_FIELD).getAsString();
        long size = assetData.get(SIZE_FIELD).getAsLong();

        if (hash == null || hash.isEmpty()) {
            throw new DownloadException(String.format("No hash provided for %s", assetPath));
        }

        IFileVerifier verifier;

        // Check if the hash is a SHA-1 hash (40 characters long)
        if (hash.length() == 40) {
            verifier = new SHA1FileVerifier(hash);
        } else {
            Utils.getLogger().warning(String.format("Using file size for validation of asset %s", assetPath));
            verifier = new FileSizeVerifier(size);
        }

        File target = new File(String.format("%s/objects/%s", assetsDirectory, hash.substring(0, 2)), hash);
        String url = MojangUtils.getResourceUrl(hash);

        Files.createDirectories(target.getParentFile().toPath());

        File cloneTo = null;

        if (isVirtual) {
            cloneTo = new File(String.format("%s/virtual/%s/%s", assetsDirectory, assetsKey, assetPath));
        } else if (mapToResources) {
            cloneTo = new File(modpack.getResourcesDir(), assetPath);
        }

        EnsureFileTask<MojangVersion> ensureFileTask = new EnsureFileTask<>(downloadAssetsQueue, target)
                .withUrl(url)
                .withVerifier(verifier);

        checkAssetsQueue.addTask(ensureFileTask);

        if (cloneTo != null && !cloneTo.exists()) {
            Files.createDirectories(cloneTo.getParentFile().toPath());
            copyAssetsQueue.addTask(new CopyFileTask<>(target, cloneTo));
        }
    }
}
