package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.CopyFileTask;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.FmlLibsManager;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.rest.io.Modpack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class InstallFmlLibsTask implements IInstallTask<MojangVersion> {
    private final ModpackModel pack;
    private final LauncherFileSystem fileSystem;
    private final Modpack modpack;
    private final ITasksQueue<MojangVersion> verifyingFiles;
    private final ITasksQueue<MojangVersion> downloadLibraryQueue;
    private final ITasksQueue<MojangVersion> copyLibraryQueue;

    public InstallFmlLibsTask(ModpackModel pack, LauncherFileSystem fileSystem, Modpack modpack,
                              ITasksQueue<MojangVersion> verifyingFiles,
                              ITasksQueue<MojangVersion> downloadLibraryQueue,
                              ITasksQueue<MojangVersion> copyLibraryQueue) {
        this.pack = pack;
        this.fileSystem = fileSystem;
        this.modpack = modpack;
        this.verifyingFiles = verifyingFiles;
        this.downloadLibraryQueue = downloadLibraryQueue;
        this.copyLibraryQueue = copyLibraryQueue;
    }

    @Override
    public String getTaskDescription() {
        return "Installing FML libraries";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<MojangVersion> queue) throws IOException, InterruptedException {
        final String minecraft = modpack.getGameVersion();

        // Add legacy FML libs
        Map<String, String> fmlLibs = FmlLibsManager.getLibsForVersion(minecraft);

        if (fmlLibs.isEmpty()) {
            return;
        }

        File modpackFmlLibDir = new File(pack.getInstalledDirectory(), "lib");
        File fmlLibsCache = new File(fileSystem.getCacheDirectory(), "fmllibs");
        Files.createDirectories(fmlLibsCache.toPath());

        fmlLibs.forEach((name, sha1) -> {
            SHA1FileVerifier verifier = null;

            if (!sha1.isEmpty()) {
                verifier = new SHA1FileVerifier(sha1);
            }

            File cached = new File(fmlLibsCache, name);
            File target = new File(modpackFmlLibDir, name);

            if (!target.exists() || (verifier != null && !verifier.isFileValid(target))) {
                EnsureFileTask<MojangVersion> ensureFileTask = new EnsureFileTask<>(downloadLibraryQueue, cached)
                        .withUrl(TechnicConstants.TECHNIC_FML_LIB_REPO + name)
                        .withVerifier(verifier);

                verifyingFiles.addTask(ensureFileTask);
                copyLibraryQueue.addTask(new CopyFileTask<>(cached, target));
            }
        });
    }
}
