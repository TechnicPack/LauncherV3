package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.ExtractRulesFileFilter;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.maven.MavenConnector;
import net.technicpack.utilslib.OperatingSystem;

import java.io.File;
import java.io.IOException;

public class InstallVersionLibTask extends ListenerTask {
    private Library library;
    private MavenConnector mavenConnector;
    private ITasksQueue grabQueue;
    private ITasksQueue downloadLibraryQueue;
    private ITasksQueue copyLibraryQueue;
    private ModpackModel pack;
    private LauncherDirectories directories;

    public InstallVersionLibTask(Library library, MavenConnector mavenConnector, ITasksQueue grabQueue, ITasksQueue downloadLibraryQueue, ITasksQueue copyLibraryQueue, ModpackModel pack, LauncherDirectories directories) {
        this.library = library;
        this.mavenConnector = mavenConnector;
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
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        super.runTask(queue);

        if (library.getUrl() != null && mavenConnector.attemptLibraryDownload(library.getName(), library.getUrl(), this))
            return;

        String[] nameBits = library.getName().split(":", 3);
        String libraryName = nameBits[1] + "-" + nameBits[2] + ".jar";
        queue.refreshProgress();

        String natives = null;
        File extractDirectory = null;
        if (library.getNatives() != null) {
            natives = library.getNatives().get(OperatingSystem.getOperatingSystem());

            if (natives != null) {
                extractDirectory = new File(this.pack.getBinDir(), "natives");
            }
        }

        String path = library.getArtifactPath(natives).replace("${arch}", System.getProperty("sun.arch.data.model"));

        File cache = new File(directories.getCacheDirectory(), path);
        if (cache.getParentFile() != null) {
            cache.getParentFile().mkdirs();
        }

        ValidZipFileVerifier zipVerifier = new ValidZipFileVerifier();
        if (cache.exists() && zipVerifier.isFileValid(cache) && extractDirectory == null)
            return;

        IFileVerifier verifier = null;
        String url = null;

        if (!cache.exists() || !zipVerifier.isFileValid(cache)) {
            url = library.getDownloadUrl(path, queue.getMirrorStore()).replace("${arch}", System.getProperty("sun.arch.data.model"));
            String md5 = queue.getMirrorStore().getETag(url);
            if (md5 != null && !md5.isEmpty()) {
                verifier = new MD5FileVerifier(md5);
            } else {
                verifier = zipVerifier;
            }
        }

        IZipFileFilter filter = null;

        if (library.getExtract() != null)
            filter = new ExtractRulesFileFilter(library.getExtract());

        grabQueue.addTask(new EnsureFileTask(cache, verifier, extractDirectory, url, downloadLibraryQueue, copyLibraryQueue, filter));
    }
}
