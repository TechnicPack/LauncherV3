package net.technicpack.minecraftcore.install;

import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ModpackZipFilter implements IZipFileFilter {
    private final ModpackModel pack;
    private final Path cacheDirPath;
    private final Path binPath;
    private final ArrayList<Path> allowedInBinDir;

    public ModpackZipFilter(ModpackModel pack) {
        this.pack = pack;

        this.cacheDirPath = this.pack.getCacheDir().toPath();
        this.binPath = this.pack.getBinDir().toPath();

        // Only some files in bin/ are allowed: modpack.jar, version.json, runData
        allowedInBinDir = new ArrayList<>();
        Stream.of("modpack.jar", "version.json", "runData").forEach(filename -> allowedInBinDir.add(this.binPath.resolve(filename)));
    }

    private void warnAboutFile(File file) {
        Utils.getLogger().log(Level.WARNING, "Modpack " + this.pack.getName() + " tried to extract reserved file " + file.getAbsolutePath());
    }

    @Override
    public boolean shouldExtract(String fileName) {
        // Prevent some special modpack-related files from being replaced when extracting the modpack:
        // - Anything in the bin/ folder, besides modpack.jar, version.json and runData
        //     - modpack.jar is used to inject things into the Minecraft jar
        //     - version.json is usually not used, but Fabric uses this to distribute itself
        //     - runData is used to specify the minimum RAM and Java version for non-Solder modpacks
        //     - anything else is removed for compatibility issues between different OSs and systems
        // - The cache/ folder and anything inside it, which the launcher uses as storage for the modpack mods
        File file = new File(this.pack.getInstalledDirectory(), fileName);

        // We use a Path here so we can properly compare paths instead of Java trying to compare them as strings,
        // which would match stuff like "cachefoo" in the "cache" dir check
        Path path = file.toPath().normalize();

        // Check if file is in $MODPACK/bin/ and not one of the allowed files
        if (path.startsWith(binPath) && !path.equals(binPath) && !allowedInBinDir.contains(path)) {
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
