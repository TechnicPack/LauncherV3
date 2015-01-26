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
import net.technicpack.launchercore.util.DownloadListener;

import java.io.IOException;

public abstract class ListenerTask implements IInstallTask, DownloadListener {

    private float taskProgress;
    private InstallTasksQueue queue;

    public ListenerTask() {
        taskProgress = 0;
    }

    @Override
    public float getTaskProgress() {
        return this.taskProgress;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        this.queue = queue;
    }

    protected void setQueue(InstallTasksQueue queue) {
        this.queue = queue;
    }

    public void stateChanged(String fileName, float progress) {
        this.taskProgress = progress;
        this.queue.refreshProgress();
    }
}
