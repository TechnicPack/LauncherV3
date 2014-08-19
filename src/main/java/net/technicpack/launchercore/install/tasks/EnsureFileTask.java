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

package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;

import java.io.File;
import java.io.IOException;

public class EnsureFileTask implements IInstallTask {
	private final File cacheLocation;
	private final File zipExtractLocation;
	private final String sourceUrl;
	private final String friendlyFileName;
    private final IFileVerifier fileVerifier;
    private final ITasksQueue downloadTaskQueue;
    private final ITasksQueue copyTaskQueue;

	public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, ITasksQueue downloadTaskQueue, ITasksQueue copyTaskQueue) {
		this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, fileLocation.getName(), downloadTaskQueue, copyTaskQueue);
	}
	public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, ITasksQueue downloadTaskQueue, ITasksQueue copyTaskQueue) {
		this.cacheLocation = fileLocation;
		this.zipExtractLocation = zipExtractLocation;
		this.sourceUrl = sourceUrl;
		this.fileVerifier = fileVerifier;
		this.friendlyFileName = friendlyFileName;
        this.downloadTaskQueue = downloadTaskQueue;
        this.copyTaskQueue = copyTaskQueue;
	}

	@Override
	public String getTaskDescription() {
		return "Verifying "+this.cacheLocation.getName();
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		if (this.zipExtractLocation != null)
			copyTaskQueue.addNextTask(new UnzipFileTask(this.cacheLocation, this.zipExtractLocation));

		if (sourceUrl != null && (!this.cacheLocation.exists() || (fileVerifier != null && !fileVerifier.isFileValid(this.cacheLocation))))
			downloadTaskQueue.addNextTask(new DownloadFileTask(this.sourceUrl, this.cacheLocation, this.fileVerifier, this.friendlyFileName));
	}
}
