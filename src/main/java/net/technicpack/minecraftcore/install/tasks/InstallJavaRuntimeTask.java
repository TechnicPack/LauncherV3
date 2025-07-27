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

import com.google.gson.JsonParseException;
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
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeFile;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeFileType;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeManifest;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.Download;
import net.technicpack.minecraftcore.mojang.version.io.VersionJavaInfo;
import net.technicpack.utilslib.OperatingSystem;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class InstallJavaRuntimeTask implements IInstallTask<IMinecraftVersionInfo> {
    private final Path runtimesDirectory;
    private final Path runtimeManifestFile;
    private final VersionJavaInfo runtimeInfo;
    private final ITasksQueue<IMinecraftVersionInfo> examineJavaQueue;
    private final ITasksQueue<IMinecraftVersionInfo> downloadJavaQueue;

    public InstallJavaRuntimeTask(Path runtimesDirectory, Path runtimeManifestFile, VersionJavaInfo runtimeInfo,
                                  ITasksQueue<IMinecraftVersionInfo> examineJavaQueue,
                                  ITasksQueue<IMinecraftVersionInfo> downloadJavaQueue) {
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

    @Override
    public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException {
        JavaRuntimeManifest manifest;

        try (Reader reader = Files.newBufferedReader(runtimeManifestFile, StandardCharsets.UTF_8)) {
            manifest = MojangUtils.getGson().fromJson(reader, JavaRuntimeManifest.class);
        } catch (JsonParseException ex) {
            throw new IOException("Failed to parse Java runtime manifest", ex);
        }

        if (manifest == null) {
            throw new DownloadException("The Java runtime manifest is invalid.");
        }

        // Create the runtime directory if it doesn't exist
        Path runtimeRoot = runtimesDirectory.resolve(runtimeInfo.getComponent());
        ensurePathIsSafe(runtimesDirectory, runtimeRoot);
        Files.createDirectories(runtimeRoot);

        // First, create the dirs
        processDirectories(manifest, runtimeRoot);

        // Then, download the files
        processFiles(manifest, runtimeRoot);

        // Then, create the links
        processSymlinks(manifest, runtimeRoot);

        // Set the Mojang JRE as the Java runtime associated to this modpack
        final IJavaRuntime runtime = getJavaRuntime(runtimeRoot);
        IMinecraftVersionInfo version = queue.getMetadata();
        version.setJavaRuntime(runtime);
    }

    private void ensurePathIsSafe(Path root, Path target) {
        Path normalizedRoot = root.normalize();
        Path normalizedTarget = target.normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new SecurityException(
                    String.format("JRE entry attempted to be placed outside of JRE root folder: %s", target)
            );
        }
    }

    private void processDirectories(JavaRuntimeManifest manifest, Path runtimeRoot) throws IOException {
        String path;
        JavaRuntimeFile runtimeFile;
        for (Map.Entry<String, JavaRuntimeFile> entry : manifest.getFiles().entrySet()) {
            path = entry.getKey();
            runtimeFile = entry.getValue();

            // We're only interested in the dirs for now
            if (runtimeFile.getType() != JavaRuntimeFileType.DIRECTORY) {
                continue;
            }

            Path dir = runtimeRoot.resolve(path);

            ensurePathIsSafe(runtimeRoot, dir);

            Files.createDirectories(dir);
        }
    }

    private void processFiles(JavaRuntimeManifest manifest, Path runtimeRoot) throws IOException {
        String path;
        JavaRuntimeFile runtimeFile;
        for (Map.Entry<String, JavaRuntimeFile> entry : manifest.getFiles().entrySet()) {
            path = entry.getKey();
            runtimeFile = entry.getValue();

            // We're only interested in the files right now
            if (runtimeFile.getType() != JavaRuntimeFileType.FILE) {
                continue;
            }

            Path target = runtimeRoot.resolve(path);
            ensurePathIsSafe(runtimeRoot, target);

            // Apparently the Mac Java 8 JRE spec doesn't have any directory entries, so we have to create them
            // regardless
            Files.createDirectories(target.getParent());

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

            EnsureFileTask<IMinecraftVersionInfo> ensureFileTask = new EnsureFileTask<>(downloadJavaQueue, target)
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
    }

    private void processSymlinks(JavaRuntimeManifest manifest, Path runtimeRoot) {
        String path;
        JavaRuntimeFile runtimeFile;
        for (Map.Entry<String, JavaRuntimeFile> entry : manifest.getFiles().entrySet()) {
            path = entry.getKey();
            runtimeFile = entry.getValue();

            // We're only interested in links right now
            if (runtimeFile.getType() != JavaRuntimeFileType.LINK) {
                continue;
            }

            Path link = runtimeRoot.resolve(path);
            ensurePathIsSafe(runtimeRoot, link);

            Path target = runtimeRoot.resolve(runtimeFile.getTarget());
            ensurePathIsSafe(runtimeRoot, target);

            // We add it to the download queue so it runs after all the files exist
            downloadJavaQueue.addTask(new EnsureLinkedFileTask<>(link, target));
        }
    }

    private static @NotNull IJavaRuntime getJavaRuntime(Path runtimeRoot) {
        final OperatingSystem os = OperatingSystem.getOperatingSystem();
        final Path runtimeExecutable;

        if (os == OperatingSystem.WINDOWS) {
            runtimeExecutable = runtimeRoot.resolve("bin/javaw.exe");
        } else if (os == OperatingSystem.OSX) {
            runtimeExecutable = runtimeRoot.resolve("jre.bundle/Contents/Home/bin/java");
        } else {
            runtimeExecutable = runtimeRoot.resolve("bin/java");
        }

        return new FileBasedJavaRuntime(runtimeExecutable);
    }
}
