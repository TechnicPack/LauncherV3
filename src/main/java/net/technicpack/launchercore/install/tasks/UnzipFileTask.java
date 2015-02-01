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

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;

public class UnzipFileTask extends ListenerTask {
    private File zipFile;
    private File destination;
    private IZipFileFilter filter;

    public UnzipFileTask(File zipFile, File destination, IZipFileFilter filter) {
        this.zipFile = zipFile;
        this.destination = destination;
        this.filter = filter;
    }

    @Override
    public String getTaskDescription() {
        return "Unzipping " + this.zipFile.getName();
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        super.runTask(queue);

        if (!zipFile.exists()) {
            throw new ZipException("Attempting to extract file " + zipFile.getName() + ", but it did not exist.");
        }

        if (!destination.exists()) {
            destination.mkdirs();
        }

        try {
            ZipUtils.unzipFile(zipFile, destination, filter, this);
        } catch (ZipException ex) {
            throw new ZipException("Error extracting file "+zipFile.getName()+".");
        }
    }
}
