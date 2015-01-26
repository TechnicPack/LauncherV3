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

package net.technicpack.autoupdate.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.IInstallTask;

import java.io.File;
import java.io.IOException;

public class EnsureUpdateFolders implements IInstallTask {
    private String taskDescription;
    private LauncherDirectories directories;

    public EnsureUpdateFolders(String taskDescription, LauncherDirectories directories) {
        this.taskDescription = taskDescription;
        this.directories = directories;
    }

    @Override
    public String getTaskDescription() {
        return taskDescription;
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        File launcherAssets = new File(directories.getAssetsDirectory(), "launcher");
        File patches = new File(launcherAssets, "patches");

        if (!patches.exists())
            patches.mkdirs();
    }
}
