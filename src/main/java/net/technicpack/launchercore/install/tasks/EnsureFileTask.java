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

import java.io.File;
import java.io.IOException;

public class EnsureFileTask<TaskQueue extends ITasksQueue> implements IInstallTask {
    private final File cacheLocation;
    private final File zipExtractLocation;
    private final String sourceUrl;
    private final String friendlyFileName;
    private final IFileVerifier fileVerifier;
    private final TaskQueue downloadTaskQueue;
    private final TaskQueue copyTaskQueue;
    private final IZipFileFilter filter;

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, TaskQueue downloadTaskQueue, TaskQueue copyTaskQueue) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, fileLocation.getName(), downloadTaskQueue, copyTaskQueue, null);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, TaskQueue downloadTaskQueue, TaskQueue copyTaskQueue, IZipFileFilter filter) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, fileLocation.getName(), downloadTaskQueue, copyTaskQueue, filter);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, TaskQueue downloadTaskQueue, TaskQueue copyTaskQueue) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, friendlyFileName, downloadTaskQueue, copyTaskQueue, null);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, TaskQueue downloadTaskQueue, TaskQueue copyTaskQueue, IZipFileFilter fileFilter) {
        this.cacheLocation = fileLocation;
        this.zipExtractLocation = zipExtractLocation;
        this.sourceUrl = sourceUrl;
        this.fileVerifier = fileVerifier;
        this.friendlyFileName = friendlyFileName;
        this.downloadTaskQueue = downloadTaskQueue;
        this.copyTaskQueue = copyTaskQueue;
        this.filter = fileFilter;
    }

    @Override
    public String getTaskDescription() {
        return "Verifying " + this.cacheLocation.getName();
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        if (this.zipExtractLocation != null)
            unzipFile(this.copyTaskQueue, this.cacheLocation, this.zipExtractLocation, this.filter);

        if (sourceUrl != null && (!this.cacheLocation.exists() || (fileVerifier != null && !fileVerifier.isFileValid(this.cacheLocation))))
            downloadFile(this.downloadTaskQueue, this.sourceUrl, this.cacheLocation, this.fileVerifier, this.friendlyFileName);
    }

    public void unzipFile(TaskQueue taskQueue, File zipLocation, File targetLocation, IZipFileFilter filter) {
        taskQueue.addNextTask(new UnzipFileTask(zipLocation, targetLocation, filter));
    }

    public void downloadFile(TaskQueue taskQueue, String sourceUrl, File targetLocation, IFileVerifier verifier, String fileName) {
        taskQueue.addNextTask(new DownloadFileTask(sourceUrl, targetLocation, verifier, fileName));
    }
}
