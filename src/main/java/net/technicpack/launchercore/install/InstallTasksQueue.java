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

import io.sentry.Sentry;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.util.DownloadListener;

import java.io.IOException;
import java.util.LinkedList;

public class InstallTasksQueue<T> implements ITasksQueue<T> {
    private final DownloadListener listener;
    private final LinkedList<IInstallTask<T>> tasks;
    private IInstallTask<T> currentTask;
    private T metadata;

    public InstallTasksQueue(DownloadListener listener) {
        this.listener = listener;
        this.tasks = new LinkedList<>();
        this.currentTask = null;
    }

    public void refreshProgress() {
        if (listener != null)
            listener.stateChanged(currentTask.getTaskDescription(), currentTask.getTaskProgress());
    }

    public void runAllTasks() throws IOException, InterruptedException {
        while (!tasks.isEmpty()) {
            currentTask = tasks.removeFirst();
            Sentry.addBreadcrumb(String.format("Running task: \"%s\" (%s)", currentTask.getTaskDescription(), currentTask.getClass().getSimpleName()));
            refreshProgress();
            currentTask.runTask(this);
        }
    }

    public void addNextTask(IInstallTask<T> task) {
        tasks.addFirst(task);
    }

    public void addTask(IInstallTask<T> task) {
        tasks.addLast(task);
    }

    public DownloadListener getDownloadListener() {
        return this.listener;
    }

    public void setMetadata(T metadata) { this.metadata = metadata; }

    public T getMetadata() { return this.metadata; }
}
