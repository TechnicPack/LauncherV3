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

import io.sentry.Sentry;
import net.technicpack.launchercore.install.IWeightedTasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;

import java.io.IOException;
import java.util.*;

public class TaskGroup<T> implements IWeightedTasksQueue<T>, IInstallTask<T> {
    private final String groupName;
    private final LinkedList<IInstallTask<T>> taskList = new LinkedList<>();
    private final Map<IInstallTask<T>, Float> taskWeights = new HashMap<>();
    private final Object taskListLock = new Object();

    private float totalWeight = 0;
    private float completedWeight = 0;

    private IInstallTask<T> currentTask;
    private String currentTaskDescription = "";

    public TaskGroup(String name) {
        this.groupName = name;
    }

    @Override
    public String getTaskDescription() {
        try {
            return String.format(Locale.ENGLISH, groupName, currentTaskDescription);
        } catch (IllegalFormatException e) {
            return groupName;
        }
    }

    @Override
    public float getTaskProgress() {
        synchronized (taskListLock) {
            if (totalWeight == 0) {
                return 0;
            }

            // The formula is:
            // progress = (completedWeight + (currentTaskFractionDone * currentTaskWeight)) / totalWeight * 100

            float progress = completedWeight;

            if (currentTask != null) {
                float currentTaskWeight = taskWeights.getOrDefault(currentTask, 1.0f);
                progress += (currentTask.getTaskProgress() / 100.0f) * currentTaskWeight;
            }

            return (progress / totalWeight) * 100.0f;
        }
    }

    @Override
    public void runTask(InstallTasksQueue<T> queue) throws IOException, InterruptedException {
        // The check is in the first synchronized block because isEmpty() isn't atomic
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            synchronized (taskListLock) {
                if (taskList.isEmpty()) {
                    break;
                }
                currentTask = taskList.removeFirst();
                currentTaskDescription = currentTask.getTaskDescription();
            }

            Sentry.addBreadcrumb(String.format("TaskGroup \"%s\" running task \"%s\"", groupName,
                                               currentTaskDescription));
            currentTask.runTask(queue);
            Sentry.addBreadcrumb(String.format("TaskGroup \"%s\" finished task \"%s\"", groupName,
                                               currentTaskDescription));

            synchronized (taskListLock) {
                queue.refreshProgress();
                completedWeight += taskWeights.getOrDefault(currentTask, 1.0f);
                taskWeights.remove(currentTask);
            }
        }
    }

    @Override
    public void addNextTask(IInstallTask<T> task) {
        addNextTask(task, 1.0f);
    }

    @Override
    public void addTask(IInstallTask<T> task) {
        addTask(task, 1.0f);
    }

    @Override
    public void addTask(IInstallTask<T> task, float weight) {
        synchronized (taskListLock) {
            taskList.addLast(task);
            taskWeights.put(task, weight);
            totalWeight += weight;
        }
    }

    @Override
    public void addNextTask(IInstallTask<T> task, float weight) {
        synchronized (taskListLock) {
            taskList.addFirst(task);
            taskWeights.put(task, weight);
            totalWeight += weight;
        }
    }


}
