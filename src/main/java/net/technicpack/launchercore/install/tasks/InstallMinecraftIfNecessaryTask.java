package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.util.DownloadUtils;
import net.technicpack.launchercore.util.MD5Utils;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.ZipUtils;

import java.io.File;
import java.io.IOException;

public class InstallMinecraftIfNecessaryTask extends ListenerTask {
	private InstalledPack pack;
	private String minecraftVersion;

	public InstallMinecraftIfNecessaryTask(InstalledPack pack, String minecraftVersion) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
	}

	@Override
	public String getTaskDescription() {
		return "Installing Minecraft";
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		super.runTask(queue);

		String url = MojangConstants.getVersionDownload(this.minecraftVersion);
		String md5 = DownloadUtils.getETag(url);
		File cache = new File(Utils.getCacheDirectory(), "minecraft_" + this.minecraftVersion + ".jar");

		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			DownloadUtils.downloadFile(url, cache.getName(), output, cache, md5, this);
		}

		ZipUtils.copyMinecraftJar(cache, new File(this.pack.getBinDir(), "minecraft.jar"));
	}
}
