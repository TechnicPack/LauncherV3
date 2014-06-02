package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.ZipUtils;
import net.technicpack.launchercore.util.verifiers.IFileVerifier;
import net.technicpack.launchercore.util.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.util.verifiers.ValidZipFileVerifier;

import java.io.File;
import java.io.IOException;

public class InstallMinecraftIfNecessaryTask extends ListenerTask {
    private InstalledPack pack;
    private String minecraftVersion;

    public InstallMinecraftIfNecessaryTask(InstalledPack pack, String minecraftVersion) {
        this.pack = pack;
        this.minecraftVersion = minecraftVersion;
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
        File cache = new File(Utils.getCacheDirectory(), "minecraft_" + this.minecraftVersion + ".jar");

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
