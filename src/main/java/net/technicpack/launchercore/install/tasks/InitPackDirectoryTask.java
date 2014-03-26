package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;

import java.io.IOException;

public class InitPackDirectoryTask implements IInstallTask {
	private ModpackModel pack;

	public InitPackDirectoryTask(ModpackModel pack) {
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
		this.pack.initDirectories();
	}
}
