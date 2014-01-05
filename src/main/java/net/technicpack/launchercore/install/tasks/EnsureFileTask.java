package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.minecraft.ExtractRules;
import net.technicpack.launchercore.util.MD5Utils;

import java.io.File;
import java.io.IOException;

public class EnsureFileTask implements IInstallTask {
	private File cacheLocation;
	private File zipExtractLocation;
	private String sourceUrl;
	private String md5;
	private ExtractRules rules;
	private String friendlyFileName;

	public EnsureFileTask(File fileLocation, File zipExtractLocation, String sourceUrl, String friendlyFileName) {
		this(fileLocation, zipExtractLocation, sourceUrl, friendlyFileName, null, null);
	}

	public EnsureFileTask(File fileLocation, File zipExtractLocation, String sourceUrl) {
		this(fileLocation, zipExtractLocation, sourceUrl, fileLocation.getName(), null, null);
	}

	public EnsureFileTask(File fileLocation, File zipExtractLocation, String sourceUrl, String md5, ExtractRules rules) {
		this(fileLocation, zipExtractLocation, sourceUrl, fileLocation.getName(), md5, rules);
	}

	public EnsureFileTask(File fileLocation, File zipExtractLocation, String sourceUrl, String friendlyFileName, String md5, ExtractRules rules) {
		this.cacheLocation = fileLocation;
		this.zipExtractLocation = zipExtractLocation;
		this.sourceUrl = sourceUrl;
		this.md5 = md5;
		this.rules = rules;
		this.friendlyFileName = friendlyFileName;
	}

	@Override
	public String getTaskDescription() {
		return "Verifying "+this.cacheLocation.getName();
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue queue) throws IOException {
		if (this.zipExtractLocation != null)
			queue.AddNextTask(new UnzipFileTask(this.cacheLocation, this.zipExtractLocation, this.rules));

		if (!this.cacheLocation.exists() || (this.md5 != null && !this.md5.isEmpty() && !MD5Utils.checkMD5(this.cacheLocation, this.md5)))
			queue.AddNextTask(new DownloadFileTask(this.sourceUrl, this.cacheLocation, this.friendlyFileName));
	}
}
