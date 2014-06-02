package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.restful.Modpack;
import net.technicpack.launchercore.restful.solder.Mod;
import net.technicpack.launchercore.util.verifiers.IFileVerifier;
import net.technicpack.launchercore.util.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.util.verifiers.ValidZipFileVerifier;

import java.io.File;
import java.io.IOException;

public class InstallModpackTask implements IInstallTask {
    private InstalledPack pack;
    private Modpack modpack;

    public InstallModpackTask(InstalledPack pack, Modpack modpack) {
        this.pack = pack;
        this.modpack = modpack;
    }

    @Override
    public String getTaskDescription() {
        return "Wiping Folders";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        File modsDir = this.pack.getModsDir();

        if (modsDir != null && modsDir.exists()) {
            deleteMods(modsDir);
        }

        File coremodsDir = this.pack.getCoremodsDir();

        if (coremodsDir != null && coremodsDir.exists()) {
            deleteMods(coremodsDir);
        }

        //HACK - jamioflan is a big jerk who needs to put his mods in the dang mod directory!
        File flansDir = new File(this.pack.getInstalledDirectory(), "Flan");

        if (flansDir.exists()) {
            deleteMods(flansDir);
        }

        File packOutput = this.pack.getInstalledDirectory();
        for (Mod mod : modpack.getMods()) {
            String url = mod.getUrl();
            String md5 = mod.getMd5();
            String name = mod.getName() + "-" + mod.getVersion() + ".zip";

            File cache = new File(this.pack.getCacheDir(), name);

            IFileVerifier verifier = null;

            if (md5 != null && !md5.isEmpty())
                verifier = new MD5FileVerifier(md5);
            else
                verifier = new ValidZipFileVerifier();

            queue.AddNextTask(new EnsureFileTask(cache, verifier, packOutput, url));
        }

        queue.AddTask(new CleanupModpackCacheTask(this.pack, modpack));
    }

    private void deleteMods(File modsDir) throws CacheDeleteException {
        for (File mod : modsDir.listFiles()) {
            if (mod.isDirectory()) {
                deleteMods(mod);
                continue;
            }

            if (mod.getName().endsWith(".zip") || mod.getName().endsWith(".jar") || mod.getName().endsWith(".litemod")) {
                if (!mod.delete()) {
                    throw new CacheDeleteException(mod.getAbsolutePath());
                }
            }
        }
    }
}
