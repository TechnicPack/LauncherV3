package net.technicpack.minecraftcore.install.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.EnsureFileTask;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.ExtractRulesFileFilter;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;

public class InstallVersionLibTask extends ListenerTask<IMinecraftVersionInfo> {
  private Library library;
  private ITasksQueue<IMinecraftVersionInfo> grabQueue;
  private ITasksQueue<IMinecraftVersionInfo> downloadLibraryQueue;
  private ITasksQueue<IMinecraftVersionInfo> copyLibraryQueue;
  private ModpackModel pack;
  private LauncherFileSystem fileSystem;

  public InstallVersionLibTask(
      Library library,
      ITasksQueue<IMinecraftVersionInfo> grabQueue,
      ITasksQueue<IMinecraftVersionInfo> downloadLibraryQueue,
      ITasksQueue<IMinecraftVersionInfo> copyLibraryQueue,
      ModpackModel pack,
      LauncherFileSystem fileSystem) {
    this.library = library;
    this.downloadLibraryQueue = downloadLibraryQueue;
    this.copyLibraryQueue = copyLibraryQueue;
    this.grabQueue = grabQueue;
    this.pack = pack;
    this.fileSystem = fileSystem;
  }

  @Override
  public String getTaskDescription() {
    return library.getName();
  }

  @Override
  public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue)
      throws IOException, InterruptedException {
    super.runTask(queue);

    queue.refreshProgress();

    // Local libraries (MMC-hint: local) live in the modpack's libraries/ directory
    if (library.isLocal()) {
      Path localPath = library.resolveLocalPath(pack.getInstalledDirectory().toPath());
      if (localPath != null) {
        return;
      }
      throw new IOException(
          "Local library "
              + library.getName()
              + " not found in "
              + pack.getInstalledDirectory().toPath().resolve("libraries"));
    }

    IMinecraftVersionInfo version = queue.getMetadata();
    String nativeClassifier =
        library.resolveNativeClassifier(
            OperatingSystem.getOperatingSystem().getName(), version.getJavaRuntime().getOsArch());
    File extractDirectory =
        nativeClassifier != null ? new File(this.pack.getBinDir(), "natives") : null;
    final String bitness = version.getJavaRuntime().getBitness();

    String path = library.getArtifactPath(nativeClassifier).replace("${arch}", bitness);

    Path cache = fileSystem.getCacheDirectory().resolve(path);

    if (cache.getParent() != null) {
      Files.createDirectories(cache.getParent());
    }

    IFileVerifier verifier;

    String sha1 = library.getArtifactSha1(nativeClassifier);
    if (sha1 != null && !sha1.isEmpty()) verifier = new SHA1FileVerifier(sha1);
    else verifier = new ValidZipFileVerifier();

    // TODO: Add check based on size (so it fails early if the size is different)
    if (Files.isRegularFile(cache) && verifier.isFileValid(cache) && extractDirectory == null)
      return;

    String url = null;

    // TODO: this causes verification to happen twice, for natives
    if (!Files.isRegularFile(cache) || !verifier.isFileValid(cache)) {
      url = library.getDownloadUrl(path).replace("${arch}", bitness);
      if (sha1 == null || sha1.isEmpty()) {
        String md5 = Utils.getETag(url);
        if (md5 != null && !md5.isEmpty()) {
          verifier = new MD5FileVerifier(md5);
        }
      }
    }

    IZipFileFilter filter = null;

    if (library.getExtract() != null) filter = new ExtractRulesFileFilter(library.getExtract());

    EnsureFileTask<IMinecraftVersionInfo> ensureFileTask =
        new EnsureFileTask<>(downloadLibraryQueue, cache)
            .withUrl(url)
            .withVerifier(verifier)
            .withZipFilter(filter);

    if (extractDirectory != null) {
      ensureFileTask.withExtractTo(extractDirectory, copyLibraryQueue);
    }

    grabQueue.addTask(ensureFileTask);
  }
}
