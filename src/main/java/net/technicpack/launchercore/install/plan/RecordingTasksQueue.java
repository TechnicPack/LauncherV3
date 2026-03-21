package net.technicpack.launchercore.install.plan;

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecordingTasksQueue<T> implements ITasksQueue<T> {
    private final LinkedList<IInstallTask<T>> tasks = new LinkedList<>();

    @Override
    public void addNextTask(IInstallTask<T> task) {
        tasks.addFirst(task);
    }

    @Override
    public void addTask(IInstallTask<T> task) {
        tasks.addLast(task);
    }

    public List<IInstallTask<T>> getTasks() {
        return Collections.unmodifiableList(tasks);
    }
}
