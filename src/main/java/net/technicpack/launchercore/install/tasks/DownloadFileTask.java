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

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.utilslib.Utils;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;

public class DownloadFileTask extends ListenerTask {
    private String url;
    private File destination;
    private String taskDescription;
    private IFileVerifier fileVerifier;
    private final boolean executable;

    protected File getDestination() { return destination; }

    public DownloadFileTask(String url, File destination, IFileVerifier verifier) {
        this(url, destination, verifier, destination.getName());
    }

    public DownloadFileTask(String url, File destination, IFileVerifier verifier, String taskDescription) {
        this(url, destination, verifier, taskDescription, false);
    }

    public DownloadFileTask(String url, File destination, IFileVerifier verifier, String taskDescription, boolean executable) {
        this.url = url;
        this.destination = destination;
        this.fileVerifier = verifier;
        this.taskDescription = taskDescription;
        this.executable = executable;
    }

    @Override
    public String getTaskDescription() {
        return taskDescription;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        super.runTask(queue);

        Utils.downloadFile(url, this.destination.getName(), this.destination.getAbsolutePath(), null, fileVerifier, this);

        if (!this.destination.exists()) {
            throw new DownloadException("Failed to download " + this.destination.getName() + ".");
        }

        if (this.executable) {
            if (!this.destination.setExecutable(this.executable)) {
                throw new DownloadException("Failed to set " + this.destination.getName() + " as executable");
            }
        }
    }
}
