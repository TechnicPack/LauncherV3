package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.ExtractRulesFileFilter;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.utilslib.*;

import java.io.File;
import java.io.IOException;

public class InstallVersionLibTask extends ListenerTask<MojangVersion> {
    private Library library;
    private ITasksQueue<MojangVersion> grabQueue;
    private ITasksQueue<MojangVersion> downloadLibraryQueue;
    private ITasksQueue<MojangVersion> copyLibraryQueue;
    private ModpackModel pack;
    private LauncherDirectories directories;

    public InstallVersionLibTask(Library library, ITasksQueue<MojangVersion> grabQueue, ITasksQueue<MojangVersion> downloadLibraryQueue,
                                 ITasksQueue<MojangVersion> copyLibraryQueue, ModpackModel pack, LauncherDirectories directories) {
        this.library = library;
        this.downloadLibraryQueue = downloadLibraryQueue;
        this.copyLibraryQueue = copyLibraryQueue;
        this.grabQueue = grabQueue;
        this.pack = pack;
        this.directories = directories;
    }

    @Override
    public String getTaskDescription() {
        return library.getName();
    }

    @Override
    public void runTask(InstallTasksQueue<MojangVersion> queue) throws IOException, InterruptedException {
        super.runTask(queue);

        queue.refreshProgress();

        // Native classifier as in the library's downloads -> classifiers -> $nativeClassifier
        // (the mapping of which is taken from the library's natives map)
        String nativeClassifier = null;
        File extractDirectory = null;
        if (library.getNatives() != null) {
            nativeClassifier = library.getNatives().get(OperatingSystem.getOperatingSystem());

            if (nativeClassifier != null) {
                extractDirectory = new File(this.pack.getBinDir(), "natives");
            }
        }

        MojangVersion version = queue.getMetadata();
        final String bitness = version.getJavaRuntime().getBitness();

        String path = library.getArtifactPath(nativeClassifier).replace("${arch}", bitness);

        File cache = new File(directories.getCacheDirectory(), path);
        if (cache.getParentFile() != null) {
            cache.getParentFile().mkdirs();
        }

        IFileVerifier verifier;

        String sha1 = library.getArtifactSha1(nativeClassifier);
        if (sha1 != null && !sha1.isEmpty())
            verifier = new SHA1FileVerifier(sha1);
        else
            verifier = new ValidZipFileVerifier();

        // TODO: Add check based on size (so it fails early if the size is different)
        if (cache.exists() && verifier.isFileValid(cache) && extractDirectory == null)
            return;

        String url = null;

        // TODO: this causes verification to happen twice, for natives
        if (!cache.exists() || !verifier.isFileValid(cache)) {
            url = library.getDownloadUrl(path).replace("${arch}", bitness);
            if (sha1 == null || sha1.isEmpty()) {
                String md5 = Utils.getETag(url);
                if (md5 != null && !md5.isEmpty()) {
                    verifier = new MD5FileVerifier(md5);
                }
            }
        }

        IZipFileFilter filter = null;

        if (library.getExtract() != null)
            filter = new ExtractRulesFileFilter(library.getExtract());

        EnsureFileTask<MojangVersion> ensureFileTask = new EnsureFileTask<>(downloadLibraryQueue, cache)
                .withUrl(url)
                .withVerifier(verifier)
                .withZipFilter(filter);

        if (extractDirectory != null) {
            ensureFileTask.withExtractTo(extractDirectory, copyLibraryQueue);
        }

        grabQueue.addTask(ensureFileTask);
    }
}
