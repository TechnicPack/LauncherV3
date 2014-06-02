package net.technicpack.launchercore.install.tasks;

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
    public void runTask(InstallTasksQueue queue) throws IOException {
        this.queue = queue;
    }

    protected void setQueue(InstallTasksQueue queue) {
        this.queue = queue;
    }

    public void stateChanged(String fileName, float progress) {
        this.taskProgress = progress;
        this.queue.RefreshProgress();
    }
}
