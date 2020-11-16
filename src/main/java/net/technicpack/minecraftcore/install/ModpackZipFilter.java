package net.technicpack.minecraftcore.install;

import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;

public class ModpackZipFilter implements IZipFileFilter {
    private final ModpackModel pack;
    private final Path binVersionPath;
    private final Path cacheDirPath;

    public ModpackZipFilter(ModpackModel pack) {
        this.pack = pack;

        this.binVersionPath = this.pack.getBinDir().toPath().resolve("version");
        this.cacheDirPath = this.pack.getCacheDir().toPath();
    }

    private void warnAboutFile(File file) {
        Utils.getLogger().log(Level.WARNING, "Modpack " + this.pack.getName() + " tried to extract reserved file " + file.getAbsolutePath());
    }

    @Override
    public boolean shouldExtract(String fileName) {
        /*
        Prevent some special modpack-related files from being replaced when extracting modpack/mods
        These files are:
        - bin/version, used by the launcher to keep track of the currently installed modpack version
        - the cache folder and anything inside of it, which the launcher uses as storage for the modpack mods
         */
        File file = new File(this.pack.getInstalledDirectory(), fileName);

        // We use a Path here so we can properly compare paths instead of Java trying to compare them as strings,
        // which would match stuff like "cachefoo" in the "cache" dir check
        Path path = file.toPath().normalize();

        // Check if file is $MODPACK/bin/version
        if (path.equals(this.binVersionPath)) {
            warnAboutFile(file);
            return false;
        }

        // Check if file is (in) $MODPACK/cache folder
        if (path.startsWith(this.cacheDirPath)) {
            warnAboutFile(file);
            return false;
        }

        return true;
    }
}
