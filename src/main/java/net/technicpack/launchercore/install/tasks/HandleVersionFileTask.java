package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.minecraft.Library;
import net.technicpack.launchercore.util.DownloadUtils;
import net.technicpack.launchercore.util.OperatingSystem;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class HandleVersionFileTask implements IInstallTask {
	private InstalledPack pack;

	public HandleVersionFileTask(InstalledPack pack) {
		this.pack = pack;
	}

	@Override
	public String getTaskDescription() {
		return "Processing version.";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		File versionFile = new File(this.pack.getBinDir(), "version.json");
		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		CompleteVersion version = Utils.getMojangGson().fromJson(json, CompleteVersion.class);

		if (version == null) {
			throw new DownloadException("The version.json file was invalid.");
		}

		for (Library library : version.getLibrariesForOS()) {
			// If minecraftforge is described in the libraries, skip it
			// HACK - Please let us get rid of this when we move to actually hosting forge,
			// or at least only do it if the users are sticking with modpack.jar
			if (library.getName().startsWith("net.minecraftforge:minecraftforge") ||
					library.getName().startsWith("net.minecraftforge:forge")) {
				continue;
			}

			String natives = null;
			File extractDirectory = null;
			if (library.getNatives() != null) {
				natives = library.getNatives().get(OperatingSystem.getOperatingSystem());

				if (natives != null) {
					extractDirectory = new File(this.pack.getBinDir(), "natives");
				}
			}

			String path = library.getArtifactPath(natives).replace("${arch}", System.getProperty("sun.arch.data.model"));
			String url = library.getDownloadUrl(path).replace("${arch}", System.getProperty("sun.arch.data.model"));
			String md5 = DownloadUtils.getETag(url);

			File cache = new File(Utils.getCacheDirectory(), path);
			if (cache.getParentFile() != null) {
				cache.getParentFile().mkdirs();
			}

			queue.AddTask(new EnsureFileTask(cache, extractDirectory, url, md5, library.getExtract()));
		}

		queue.AddTask(new GetAssetsIndexTask(this.pack));
		queue.setCompleteVersion(version);
	}
}
