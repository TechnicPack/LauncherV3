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

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.EnsureLinkedFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaRuntime;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeFileType;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeManifest;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.Download;
import net.technicpack.minecraftcore.mojang.version.io.VersionJavaInfo;
import net.technicpack.utilslib.OperatingSystem;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class InstallJavaRuntimeTask implements IInstallTask<MojangVersion> {
    private final ModpackModel modpack;
    private final File runtimesDirectory;
    private final File runtimeManifestFile;
    private final VersionJavaInfo runtimeInfo;
    private final ITasksQueue<MojangVersion> examineJavaQueue;
    private final ITasksQueue<MojangVersion> downloadJavaQueue;

    public InstallJavaRuntimeTask(ModpackModel modpack, File runtimesDirectory, File runtimeManifestFile, VersionJavaInfo runtimeInfo, ITasksQueue<MojangVersion> examineJavaQueue, ITasksQueue<MojangVersion> downloadJavaQueue) {
        this.modpack = modpack;
        this.runtimesDirectory = runtimesDirectory;
        this.runtimeManifestFile = runtimeManifestFile;
        this.runtimeInfo = runtimeInfo;
        this.examineJavaQueue = examineJavaQueue;
        this.downloadJavaQueue = downloadJavaQueue;
    }

    @Override
    public String getTaskDescription() {
        return "Installing Java runtime";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    private void ensurePathIsSafe(File root, File target) {
        if (!target.toPath().normalize().startsWith(root.toPath())) {
            throw new RuntimeException("JRE entry attempted to be placed outside of JRE root folder: " + target.getAbsolutePath());
        }
    }

    @Override
    public void runTask(InstallTasksQueue<MojangVersion> queue) throws IOException {
        String json = FileUtils.readFileToString(runtimeManifestFile, StandardCharsets.UTF_8);
        JavaRuntimeManifest manifest = MojangUtils.getGson().fromJson(json, JavaRuntimeManifest.class);

        if (manifest == null) {
            throw new DownloadException("The Java runtime manifest is invalid.");
        }

        // Create the runtime directory if it doesn't exist
        File runtimeRoot = new File(runtimesDirectory, runtimeInfo.getComponent()).getAbsoluteFile();
        ensurePathIsSafe(runtimesDirectory, runtimeRoot);
        runtimeRoot.mkdirs();

        // First, create the dirs
        manifest.getFiles().forEach((path, runtimeFile) -> {
            // We're only interested in the dirs for now
            if (runtimeFile.getType() == JavaRuntimeFileType.DIRECTORY) {
                File dir = new File(runtimeRoot, path);

                ensurePathIsSafe(runtimeRoot, dir);

                dir.mkdirs();
            }
        });

        // Then, download the files
        manifest.getFiles().forEach((path, runtimeFile) -> {
            // We're only interested in the files right now
            if (runtimeFile.getType() == JavaRuntimeFileType.FILE) {
                File target = new File(runtimeRoot, path);

                ensurePathIsSafe(runtimeRoot, target);

                // Apparently the Mac Java 8 JRE spec doesn't have any directory entries, so we have to create them regardless
                target.getParentFile().mkdirs();

                Download rawDownload = runtimeFile.getDownloads().getRaw();
                Download lzmaDownload = runtimeFile.getDownloads().getLzma();

                IFileVerifier verifier = new SHA1FileVerifier(rawDownload.getSha1());

                final boolean useLzma = lzmaDownload != null && !lzmaDownload.getUrl().isEmpty() && ((double) lzmaDownload.getSize() / rawDownload.getSize() <= 0.66);

                String downloadUrl;
                if (useLzma) {
                    downloadUrl = lzmaDownload.getUrl();
                } else {
                    downloadUrl = rawDownload.getUrl();
                }

                EnsureFileTask<MojangVersion> ensureFileTask = new EnsureFileTask<>(downloadJavaQueue, target)
                        .withUrl(downloadUrl)
                        .withVerifier(verifier);

                if (useLzma) {
                    ensureFileTask.withDownloadDecompressor(CompressorStreamFactory.LZMA);
                }

                if (runtimeFile.isExecutable()) {
                    ensureFileTask.withExecutableBitSet();
                }

                examineJavaQueue.addTask(ensureFileTask);
            }
        });

        // Then, create the links
        manifest.getFiles().forEach((path, runtimeFile) -> {
            // We're only interested in links right now
            if (runtimeFile.getType() == JavaRuntimeFileType.LINK) {
                File link = new File(runtimeRoot, path);
                ensurePathIsSafe(runtimeRoot, link);

                File target = new File(link, runtimeFile.getTarget());
                ensurePathIsSafe(runtimeRoot, target);

                // We add it to the download queue so it runs after all the files exist
                downloadJavaQueue.addTask(new EnsureLinkedFileTask<>(link, target));
            }
        });

        // Set the Mojang JRE as the Java runtime associated to this modpack
        final OperatingSystem os = OperatingSystem.getOperatingSystem();
        final Path runtimeExecutable;
        if (os == OperatingSystem.WINDOWS) {
            runtimeExecutable = runtimeRoot.toPath().resolve("bin/javaw.exe");
        } else if (os == OperatingSystem.OSX) {
            runtimeExecutable = runtimeRoot.toPath().resolve("jre.bundle/Contents/Home/bin/java");
        } else {
            runtimeExecutable = runtimeRoot.toPath().resolve("bin/java");
        }

        final IJavaRuntime runtime = new FileBasedJavaRuntime(runtimeExecutable);


        MojangVersion version = queue.getMetadata();

        version.setJavaRuntime(runtime);
    }
}
