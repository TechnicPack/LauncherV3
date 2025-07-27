/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.utilslib.IZipFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@SuppressWarnings("UnusedReturnValue")
public class EnsureFileTask<T> implements IInstallTask<T> {
    private final @NotNull File targetFile;
    private final @NotNull ITasksQueue<T> downloadTaskQueue;
    private final @NotNull String friendlyFileName;
    private File zipExtractionDirectory;
    private String sourceUrl;
    private IFileVerifier fileVerifier;
    private ITasksQueue<T> copyTaskQueue;
    private @Nullable IZipFileFilter filter = null;
    private boolean executable = false;
    private String downloadDecompressor;

    public EnsureFileTask(@NotNull ITasksQueue<T> downloadQueue, @NotNull File target) {
        this.downloadTaskQueue = Objects.requireNonNull(downloadQueue, "Download task queue must be set.");
        this.targetFile = Objects.requireNonNull(target, "Target file must be set.");
        this.friendlyFileName = target.getName();
    }

    public EnsureFileTask(@NotNull ITasksQueue<T> downloadQueue, @NotNull Path target) {
        this(downloadQueue, target.toFile());
    }

    @Override
    public @NotNull String getTaskDescription() {
        return String.format("Verifying %s", targetFile.getName());
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<T> queue) throws IOException {
        Objects.requireNonNull(this.downloadTaskQueue, "Download task queue must be set.");
        Objects.requireNonNull(this.targetFile, "Target file must be set.");
        Objects.requireNonNull(this.friendlyFileName, "Friendly file name must be set.");

        if (zipExtractionDirectory != null) {
            // This is added before the download because we're adding to the head, so the download task runs before
            copyTaskQueue.addNextTask(new UnzipFileTask<>(targetFile, zipExtractionDirectory, filter));
        }

        if (sourceUrl != null && (!targetFile.exists() || (fileVerifier != null && !fileVerifier.isFileValid(targetFile)))) {
            DownloadFileTask<T> downloadFileTask = new DownloadFileTask<>(sourceUrl, targetFile, fileVerifier,
                                                                          friendlyFileName);

            if (executable) {
                downloadFileTask.withExecutable();
            }

            if (downloadDecompressor != null) {
                downloadFileTask.setDecompressor(downloadDecompressor);
            }

            downloadTaskQueue.addNextTask(downloadFileTask);
        }
    }

    /**
     * Mark the target file as executable
     */
    public @NotNull EnsureFileTask<T> withExecutableBitSet() {
        this.executable = true;
        return this;
    }

    /**
     * @see DownloadFileTask#setDecompressor(String)
     */
    public @NotNull EnsureFileTask<T> withDownloadDecompressor(String downloadDecompressor) {
        this.downloadDecompressor = downloadDecompressor;
        return this;
    }

    public @NotNull EnsureFileTask<T> withVerifier(IFileVerifier fileVerifier) {
        this.fileVerifier = fileVerifier;
        return this;
    }

    /**
     * Sets the directory where to extract this ZIP file to.
     * @param directory The directory to extract into
     * @param copyQueue The {@link ITasksQueue<T>} to run the extraction operation in
     */
    public @NotNull EnsureFileTask<T> withExtractTo(@NotNull File directory, @NotNull ITasksQueue<T> copyQueue) {
        Objects.requireNonNull(directory, "Extraction directory must be set.");
        Objects.requireNonNull(copyQueue, "Copy task queue must be set.");

        this.zipExtractionDirectory = directory;
        this.copyTaskQueue = copyQueue;

        return this;
    }

    public @NotNull EnsureFileTask<T> withZipFilter(IZipFileFilter filter) {
        this.filter = filter;
        return this;
    }

    public @NotNull EnsureFileTask<T> withUrl(String url) {
        this.sourceUrl = url;
        return this;
    }
}
