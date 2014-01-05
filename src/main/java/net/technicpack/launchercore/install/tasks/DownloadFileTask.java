package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.util.DownloadUtils;

import java.io.File;
import java.io.IOException;

public class DownloadFileTask extends ListenerTask {
	private String url;
	private File destination;
	private String taskDescription;

	public DownloadFileTask(String url, File destination) {
		this(url, destination, destination.getName());
	}

	public DownloadFileTask(String url, File destination, String taskDescription) {
		this.url = url;
		this.destination = destination;
		this.taskDescription = taskDescription;
	}

	@Override
	public String getTaskDescription() {
		return this.taskDescription;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		super.runTask(queue);

		DownloadUtils.downloadFile(url, this.destination.getName(), this.destination.getAbsolutePath(), null, null, this);

		if (!this.destination.exists()) {
			throw new DownloadException("Failed to download "+this.destination.getName()+".");
		}
	}
}
