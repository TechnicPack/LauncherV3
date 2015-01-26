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

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.util.DownloadListener;

import java.io.IOException;
import java.util.LinkedList;

public class InstallTasksQueue<Metadata> implements ITasksQueue {
    private DownloadListener listener;
    private LinkedList<IInstallTask> tasks;
    private IInstallTask currentTask;
    private MirrorStore mirrorStore;
    private Metadata metadata;

    public InstallTasksQueue(DownloadListener listener, MirrorStore mirrorStore) {
        this.listener = listener;
        this.mirrorStore = mirrorStore;
        this.tasks = new LinkedList<IInstallTask>();
        this.currentTask = null;
    }

    public void refreshProgress() {
        if (listener != null)
            listener.stateChanged(currentTask.getTaskDescription(), currentTask.getTaskProgress());
    }

    public void runAllTasks() throws IOException, InterruptedException {
        while (!tasks.isEmpty()) {
            currentTask = tasks.removeFirst();
            refreshProgress();
            currentTask.runTask(this);
        }
    }

    public void addNextTask(IInstallTask task) {
        tasks.addFirst(task);
    }

    public void addTask(IInstallTask task) {
        tasks.addLast(task);
    }

    public MirrorStore getMirrorStore() {
        return this.mirrorStore;
    }

    public DownloadListener getDownloadListener() {
        return this.listener;
    }

    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    public Metadata getMetadata() { return this.metadata; }
}
