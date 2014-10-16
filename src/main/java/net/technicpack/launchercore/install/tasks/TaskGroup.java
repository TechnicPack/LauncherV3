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

import java.io.IOException;
import java.util.LinkedList;

public class TaskGroup implements ITasksQueue, IInstallTask {
    private final String groupName;
    private final LinkedList<IInstallTask> taskList = new LinkedList<IInstallTask>();

    private int taskProgress = 0;
    private String fileName = "";

    public TaskGroup(String name) {
        this.groupName = name;
    }

    @Override
    public String getTaskDescription() {
        return groupName.replace("%s", fileName);
    }

    @Override
    public float getTaskProgress() {

        if (taskList.size() == 0)
            return 0;

        float baseProgress = (100.0f/taskList.size());
        float finishedTasksProgress = baseProgress * taskProgress;
        IInstallTask currentTask = taskList.get(taskProgress);
        float currentTaskProgress = (currentTask.getTaskProgress()/100.0f)*baseProgress;
        return finishedTasksProgress + currentTaskProgress;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        while (taskProgress < taskList.size()) {
            if (Thread.interrupted())
                throw new InterruptedException();
            IInstallTask currentTask = taskList.get(taskProgress);
            fileName = currentTask.getTaskDescription();
            currentTask.runTask(queue);
            queue.refreshProgress();
            taskProgress++;
        }
    }

    @Override
    public void addNextTask(IInstallTask task) {
        taskList.addFirst(task);
    }

    @Override
    public void addTask(IInstallTask task) {
        taskList.addLast(task);
    }
}
