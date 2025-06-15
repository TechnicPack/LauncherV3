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
import net.technicpack.launchercore.install.IWeightedTasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ParallelTaskGroup implements IInstallTask, IWeightedTasksQueue {
    private final String groupName;
    private final Map<IInstallTask, Float> taskWeights = new LinkedHashMap<>();
    private final List<IInstallTask> taskList = new ArrayList<>();
    private float totalWeight = 0f;

    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicReference<String> currentFile = new AtomicReference<>("");

    private final ExecutorService executor;

    public ParallelTaskGroup(String name) {
        this.groupName = name;
        this.executor = createDefaultExecutor();
    }

    private static ExecutorService createDefaultExecutor() {
        int maxThreads = Math.min(64, Runtime.getRuntime().availableProcessors());
        return Executors.newFixedThreadPool(maxThreads, r -> {
            Thread t = new Thread(r);
            t.setName(String.format("ParallelTaskGroup-%d", t.getId()));
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (IInstallTask task : taskList) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    currentFile.set(task.getTaskDescription());
                    task.runTask(queue);
                    completedTasks.incrementAndGet();
                    queue.refreshProgress();
                } catch (IOException e) {
                    throw new CompletionException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }, executor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof InterruptedException) throw (InterruptedException) cause;
            throw new DownloadException(cause);
        } finally {
            executor.shutdown();
        }
    }

    @Override
    public String getTaskDescription() {
        return groupName.replace("%s", currentFile.get());
    }

    @Override
    public float getTaskProgress() {
        if (taskList.isEmpty() || totalWeight == 0) return 0;

        float completedWeight = 0;
        for (IInstallTask task : taskList) {
            float weight = taskWeights.getOrDefault(task, 1.0f);
            completedWeight += (task.getTaskProgress() / 100f) * weight;
        }

        return (completedWeight / totalWeight) * 100f;
    }

    @Override
    public void addTask(IInstallTask task) {
        addTask(task, 1.0f);
    }

    @Override
    public void addTask(IInstallTask task, float weight) {
        taskList.add(task);
        taskWeights.put(task, weight);
        totalWeight += weight;
    }

    @Override
    public void addNextTask(IInstallTask task) {
        addNextTask(task, 1.0f);
    }

    @Override
    public void addNextTask(IInstallTask task, float weight) {
        taskList.add(0, task);
        taskWeights.put(task, weight);
        totalWeight += weight;
    }
}
