package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.minecraft.ExtractRules;
import net.technicpack.launchercore.util.verifiers.IFileVerifier;

import java.io.File;
import java.io.IOException;

public class EnsureFileTask implements IInstallTask {
    private File cacheLocation;
    private File zipExtractLocation;
    private String sourceUrl;
    private ExtractRules rules;
    private String friendlyFileName;
    private IFileVerifier fileVerifier;

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, friendlyFileName, null);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, fileLocation.getName(), null);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, ExtractRules rules) {
        this(fileLocation, fileVerifier, zipExtractLocation, sourceUrl, fileLocation.getName(), rules);
    }

    public EnsureFileTask(File fileLocation, IFileVerifier fileVerifier, File zipExtractLocation, String sourceUrl, String friendlyFileName, ExtractRules rules) {
        this.cacheLocation = fileLocation;
        this.zipExtractLocation = zipExtractLocation;
        this.sourceUrl = sourceUrl;
        this.fileVerifier = fileVerifier;
        this.rules = rules;
        this.friendlyFileName = friendlyFileName;
    }

    @Override
    public String getTaskDescription() {
        return "Verifying " + this.cacheLocation.getName();
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        if (this.zipExtractLocation != null)
            queue.AddNextTask(new UnzipFileTask(this.cacheLocation, this.zipExtractLocation, this.rules));

        if (!this.cacheLocation.exists() || (fileVerifier != null && !fileVerifier.isFileValid(this.cacheLocation)))
            queue.AddNextTask(new DownloadFileTask(this.sourceUrl, this.cacheLocation, this.fileVerifier, this.friendlyFileName));
    }
}
