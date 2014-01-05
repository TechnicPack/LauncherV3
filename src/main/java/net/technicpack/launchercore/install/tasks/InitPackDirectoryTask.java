package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstalledPack;

import java.io.File;
import java.io.IOException;

public class InitPackDirectoryTask implements IInstallTask {
	private InstalledPack pack;

	public InitPackDirectoryTask(InstalledPack pack) {
		this.pack = pack;
	}

	@Override
	public String getTaskDescription() {
		return "Initializing Directories";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		this.pack.getInstalledDirectory();
		this.pack.initDirectories();
	}
}
