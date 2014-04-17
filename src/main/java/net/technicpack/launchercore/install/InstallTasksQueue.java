package net.technicpack.launchercore.install;

import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.minecraftcore.mojang.CompleteVersion;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.util.DownloadListener;

import java.io.IOException;
import java.util.LinkedList;

public class InstallTasksQueue implements ITasksQueue {
	private DownloadListener listener;
	private LinkedList<IInstallTask> tasks;
	private IInstallTask currentTask;
	private CompleteVersion completeVersion;
    private MirrorStore mirrorStore;

	public InstallTasksQueue(DownloadListener listener, MirrorStore mirrorStore) {
		this.listener = listener;
        this.mirrorStore = mirrorStore;
		this.tasks = new LinkedList<IInstallTask>();
		this.currentTask = null;
	}

	public void refreshProgress() {
		listener.stateChanged(currentTask.getTaskDescription(), currentTask.getTaskProgress());
	}

	public void runAllTasks() throws IOException {
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

	public void setCompleteVersion(CompleteVersion version) {
		this.completeVersion = version;
	}

	public CompleteVersion getCompleteVersion() {
		return this.completeVersion;
	}
    public MirrorStore getMirrorStore() { return this.mirrorStore; }
}
