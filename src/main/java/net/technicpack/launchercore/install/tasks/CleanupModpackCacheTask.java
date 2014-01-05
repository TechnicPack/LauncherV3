package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.restful.Modpack;
import net.technicpack.launchercore.restful.solder.Mod;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CleanupModpackCacheTask implements IInstallTask {
	private InstalledPack pack;
	private String build;

	public CleanupModpackCacheTask(InstalledPack pack, String build) {
		this.pack = pack;
		this.build = build;
	}

	@Override
	public String getTaskDescription() {
		return "Cleaning Modpack Cache";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		File[] files = this.pack.getCacheDir().listFiles();

		if (files == null) {
			return;
		}

		Modpack modpack = this.pack.getInfo().getModpack(this.build);
		Set<String> keepFiles = new HashSet<String>(modpack.getMods().size() + 1);
		for (Mod mod : modpack.getMods()) {
			keepFiles.add(mod.getName() + "-" + mod.getVersion() + ".zip");
		}
		keepFiles.add("minecraft.jar");

		for (File file : files) {
			String fileName = file.getName();
			if (keepFiles.contains(fileName)) {
				continue;
			}
			FileUtils.deleteQuietly(file);
		}
	}
}
