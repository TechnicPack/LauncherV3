package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.util.DownloadListener;
import sun.misc.Launcher;

import java.awt.Component;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class InstallTasksQueue {
	private DownloadListener listener;
	private LinkedList<IInstallTask> tasks;
	private IInstallTask currentTask;
	private CompleteVersion completeVersion;

	public InstallTasksQueue(DownloadListener listener, Component uiParent, InstalledPack pack, String build) {
		this.listener = listener;
		this.tasks = new LinkedList<IInstallTask>();
		this.currentTask = null;
	}

	public void RefreshProgress() {
		listener.stateChanged(currentTask.getTaskDescription(), currentTask.getTaskProgress());
	}

	public void RunAllTasks() throws IOException {
		while (!tasks.isEmpty()) {
			currentTask = tasks.removeFirst();
			RefreshProgress();
			currentTask.runTask(this);
		}
	}

	public void AddNextTask(IInstallTask task) {
		tasks.addFirst(task);
	}

	public void AddTask(IInstallTask task) {
		tasks.addLast(task);
	}

	public void setCompleteVersion(CompleteVersion version) {
		this.completeVersion = version;
	}

	public CompleteVersion getCompleteVersion() {
		return this.completeVersion;
	}
}
