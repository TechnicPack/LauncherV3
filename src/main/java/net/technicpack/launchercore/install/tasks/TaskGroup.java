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
import net.technicpack.launchercore.install.InstallTasksQueue;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TaskGroup implements IWeightedTasksQueue, IInstallTask {
    private final String groupName;
    private final LinkedList<IInstallTask> taskList = new LinkedList<>();
    private final Map<IInstallTask, Float> taskWeights = new HashMap<>();

    private float totalWeight = 0;
    private float completedWeight = 0;

    private String fileName = "";
    private IInstallTask currentTask;

    public TaskGroup(String name) {
        this.groupName = name;
    }

    @Override
    public String getTaskDescription() {
        return groupName.replace("%s", fileName);
    }

    @Override
    public float getTaskProgress() {
        if (taskList.isEmpty())
            return 0;
        if (totalWeight == 0)
            return 0;

        float finishedTasksProgress = (completedWeight / totalWeight);
        float currentTaskProgress;

        if (currentTask == null) {
            currentTaskProgress = 0;
        } else {
            float currentTaskWeight = taskWeights.get(currentTask);

            currentTaskProgress = (currentTask.getTaskProgress() / 100.0f) * (currentTaskWeight / totalWeight);
        }

        return (finishedTasksProgress + currentTaskProgress) * 100.0f;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        while (!taskList.isEmpty()) {
            if (Thread.interrupted())
                throw new InterruptedException();
            // Get next task
            currentTask = taskList.removeFirst();
            // Get the task weight (normally 1, but it's filesize for downloads)
            float currentTaskWeight = taskWeights.get(currentTask);
            // Get task description
            fileName = currentTask.getTaskDescription();
            // Update the progress visually before it runs
            queue.refreshProgress();
            // Run the actual taskz
            currentTask.runTask(queue);
            // Update the completed task weight
            completedWeight += currentTaskWeight;
            // Unset the current task so progress doesn't jump back when it re-renders after the task is done
            currentTask = null;
            // Update the progress visually after it's done running
            queue.refreshProgress();
        }
    }

    @Override
    public void addNextTask(IInstallTask task) {
        addNextTask(task, 1);
    }

    @Override
    public void addTask(IInstallTask task) {
        addTask(task, 1);
    }

    public void addNextTask(IInstallTask task, float weight) {
        taskList.addFirst(task);
        taskWeights.put(task, weight);
        totalWeight += weight;
    }

    public void addTask(IInstallTask task, float weight) {
        taskList.addLast(task);
        taskWeights.put(task, weight);
        totalWeight += weight;
    }
}
