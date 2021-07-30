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

import net.technicpack.launchercore.install.IWeightedTasksQueue;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.utilslib.IZipFileFilter;

import java.io.File;

public class EnsureSizedFileTask extends EnsureFileTask<IWeightedTasksQueue> {

    private int fileSize;
    public EnsureSizedFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, IWeightedTasksQueue downloadTaskQueue, IWeightedTasksQueue copyTaskQueue, int fileSize) {
        super(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, downloadTaskQueue, copyTaskQueue);
        this.fileSize = fileSize;
    }

    public EnsureSizedFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, IWeightedTasksQueue downloadTaskQueue, IWeightedTasksQueue copyTaskQueue, IZipFileFilter filter, int fileSize) {
        super(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, downloadTaskQueue, copyTaskQueue, filter);
        this.fileSize = fileSize;
    }

    public EnsureSizedFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, IWeightedTasksQueue downloadTaskQueue, IWeightedTasksQueue copyTaskQueue, int fileSize) {
        super(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, friendlyFileName, downloadTaskQueue, copyTaskQueue);
        this.fileSize = fileSize;
    }

    public EnsureSizedFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, IWeightedTasksQueue downloadTaskQueue, IWeightedTasksQueue copyTaskQueue, IZipFileFilter fileFilter, int fileSize) {
        super(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, friendlyFileName, downloadTaskQueue, copyTaskQueue, fileFilter);
        this.fileSize = fileSize;
    }

    @Override
    public void unzipFile(IWeightedTasksQueue taskQueue, File zipLocation, File targetLocation, IZipFileFilter filter) {
        taskQueue.addNextTask(new UnzipFileTask(zipLocation, targetLocation, filter), fileSize);
    }

    @Override
    public void downloadFile(IWeightedTasksQueue taskQueue, String sourceUrl, File targetLocation, IFileVerifier verifier, String fileName) {
        taskQueue.addNextTask(new DownloadFileTask(sourceUrl, targetLocation, verifier, fileName), fileSize);
    }
}
