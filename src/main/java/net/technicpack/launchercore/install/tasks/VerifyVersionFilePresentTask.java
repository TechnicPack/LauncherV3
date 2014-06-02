package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.TechnicConstants;
import net.technicpack.launchercore.util.ZipUtils;
import net.technicpack.launchercore.util.verifiers.IFileVerifier;
import net.technicpack.launchercore.util.verifiers.ValidJsonFileVerifier;

import java.io.File;
import java.io.IOException;

public class VerifyVersionFilePresentTask implements IInstallTask {
    private InstalledPack pack;
    private String minecraftVersion;

    public VerifyVersionFilePresentTask(InstalledPack pack, String minecraftVersion) {
        this.pack = pack;
        this.minecraftVersion = minecraftVersion;
    }

    @Override
    public String getTaskDescription() {
        return "Retrieving Modpack Version";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        File versionFile = new File(this.pack.getBinDir(), "version.json");
        File modpackJar = new File(this.pack.getBinDir(), "modpack.jar");

        boolean didExtract = false;

        if (modpackJar.exists()) {
            didExtract = ZipUtils.extractFile(modpackJar, this.pack.getBinDir(), "version.json");
        }

        IFileVerifier fileVerifier = new ValidJsonFileVerifier();

        if (!versionFile.exists() || !fileVerifier.isFileValid(versionFile)) {
            if (this.pack.isLocalOnly()) {
                throw new PackNotAvailableOfflineException(this.pack.getDisplayName());
            } else {
                queue.AddNextTask(new DownloadFileTask(TechnicConstants.getTechnicVersionJson(this.minecraftVersion), versionFile, fileVerifier));
            }
        }
    }
}
