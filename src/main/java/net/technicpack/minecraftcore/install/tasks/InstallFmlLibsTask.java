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
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.rest.io.Modpack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class InstallFmlLibsTask implements IInstallTask<IMinecraftVersionInfo> {
    private final ModpackModel pack;
    private final LauncherFileSystem fileSystem;
    private final Modpack modpack;
    private final ITasksQueue<IMinecraftVersionInfo> verifyingFiles;
    private final ITasksQueue<IMinecraftVersionInfo> downloadLibraryQueue;
    private final ITasksQueue<IMinecraftVersionInfo> copyLibraryQueue;

    public InstallFmlLibsTask(ModpackModel pack, LauncherFileSystem fileSystem, Modpack modpack,
                              ITasksQueue<IMinecraftVersionInfo> verifyingFiles,
                              ITasksQueue<IMinecraftVersionInfo> downloadLibraryQueue,
                              ITasksQueue<IMinecraftVersionInfo> copyLibraryQueue) {
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
    public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException, InterruptedException {
        final String minecraft = modpack.getGameVersion();

        // Add legacy FML libs
        Map<String, String> fmlLibs = FmlLibsManager.getLibsForVersion(minecraft);

        if (fmlLibs.isEmpty()) {
            return;
        }

        Path fmlLibsCache = fileSystem.getCacheDirectory().resolve("fmllibs");
        Files.createDirectories(fmlLibsCache);
        File modpackFmlLibDir = new File(pack.getInstalledDirectory(), "lib");

        fmlLibs.forEach((name, sha1) -> {
            SHA1FileVerifier verifier = null;

            if (!sha1.isEmpty()) {
                verifier = new SHA1FileVerifier(sha1);
            }

            File cached = fmlLibsCache.resolve(name).toFile();
            File target = new File(modpackFmlLibDir, name);

            if (!target.exists() || (verifier != null && !verifier.isFileValid(target))) {
                EnsureFileTask<IMinecraftVersionInfo> ensureFileTask = new EnsureFileTask<>(downloadLibraryQueue, cached)
                        .withUrl(TechnicConstants.TECHNIC_FML_LIB_REPO + name)
                        .withVerifier(verifier);

                verifyingFiles.addTask(ensureFileTask);
                copyLibraryQueue.addTask(new CopyFileTask<>(cached, target));
            }
        });
    }
}
