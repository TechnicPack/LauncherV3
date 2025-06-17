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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CopyFileTask<T> implements IInstallTask<T> {
    private File source;
    private File destination;

    public CopyFileTask(File source, File destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String getTaskDescription() {
        return "Copying files.";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<T> queue) throws IOException {
        FileUtils.copyFile(this.source, this.destination);
    }
}
