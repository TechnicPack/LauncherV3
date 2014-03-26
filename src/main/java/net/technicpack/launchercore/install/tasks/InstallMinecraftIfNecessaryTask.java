package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.MojangConstants;
import net.technicpack.utilslib.ZipUtils;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;

import java.io.File;
import java.io.IOException;

public class InstallMinecraftIfNecessaryTask extends ListenerTask {
	private ModpackModel pack;
	private String minecraftVersion;
    private File cacheDirectory;

	public InstallMinecraftIfNecessaryTask(ModpackModel pack, String minecraftVersion, File cacheDirectory) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
        this.cacheDirectory = cacheDirectory;
	}

	@Override
	public String getTaskDescription() {
		return "Installing Minecraft";
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		super.runTask(queue);

		String url = MojangConstants.getVersionDownload(this.minecraftVersion);
		String md5 = queue.getMirrorStore().getETag(url);
		File cache = new File(cacheDirectory, "minecraft_" + this.minecraftVersion + ".jar");

        IFileVerifier verifier = null;

        if (md5 != null && !md5.isEmpty()) {
            verifier = new MD5FileVerifier(md5);
        } else {
            verifier = new ValidZipFileVerifier();
        }

		if (!cache.exists() || !verifier.isFileValid(cache)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			queue.getMirrorStore().downloadFile(url, cache.getName(), output, cache, verifier, this);
		}

		ZipUtils.copyMinecraftJar(cache, new File(this.pack.getBinDir(), "minecraft.jar"));
	}
}
