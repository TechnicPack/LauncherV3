/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.FileVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.ZipFileRetriever;
import net.technicpack.minecraftcore.mojang.version.io.Artifact;
import net.technicpack.minecraftcore.mojang.version.io.Downloads;
import net.technicpack.minecraftcore.mojang.version.io.Library;

import java.io.File;
import java.io.IOException;

public class HandleVersionFileTask implements IInstallTask {
    private final ModpackModel pack;
    private final LauncherDirectories directories;
    private final ITasksQueue checkLibraryQueue;
    private final ITasksQueue downloadLibraryQueue;
    private final ITasksQueue copyLibraryQueue;
    private final ITasksQueue checkNonMavenLibsQueue;
    private final MojangVersionBuilder versionBuilder;

    private String libraryName;

    public HandleVersionFileTask(ModpackModel pack, LauncherDirectories directories, ITasksQueue checkNonMavenLibsQueue, ITasksQueue checkLibraryQueue, ITasksQueue downloadLibraryQueue, ITasksQueue copyLibraryQueue, MojangVersionBuilder versionBuilder) {
        this.pack = pack;
        this.directories = directories;
        this.checkLibraryQueue = checkLibraryQueue;
        this.downloadLibraryQueue = downloadLibraryQueue;
        this.copyLibraryQueue = copyLibraryQueue;
        this.checkNonMavenLibsQueue = checkNonMavenLibsQueue;
        this.versionBuilder = versionBuilder;
    }

    @Override
    public String getTaskDescription() {
        if (libraryName == null)
            return "Processing version.";
        else
            return "Verifying " + libraryName + ".";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        MojangVersion version = versionBuilder.buildVersionFromKey(null);

        if (version == null) {
            throw new DownloadException("The version.json file was invalid.");
        }

        // if MC < 1.6, we inject LegacyWrapper
        // HACK
        boolean isLegacy = MojangUtils.isLegacyVersion(version.getId());

        if (isLegacy) {
            Library legacyWrapper = new Library();
            legacyWrapper.setName("net.technicpack:legacywrapper:1.2.1");
            legacyWrapper.setUrl("https://mirror.technicpack.net/Technic/lib/");

            version.addLibrary(legacyWrapper);

            version.setMainClass("net.technicpack.legacywrapper.Launch");
        }

        // In Forge 1.13+ and 1.12.2 > 2847, there's an installer jar, and the universal jar can't be used since it
        // doesn't have the required dependencies to actually launch the game.
        // So, for Forge 1.13+ we need to use ForgeWrapper to install Forge (it performs deobfuscation at install time).
        // For Forge 1.12.2 it launches directly, as long as we inject the universal jar (it won't launch at all if
        // you try running it through ForgeWrapper).
        final boolean hasModernForge = MojangUtils.hasModernForge(version);

        if (hasModernForge) {
            File profileJson = new File(pack.getBinDir(), "install_profile.json");
            ZipFileRetriever zipVersionRetriever = new ZipFileRetriever(new File(pack.getBinDir(), "modpack.jar"));
            MojangVersion profileVersion = new FileVersionBuilder(profileJson, zipVersionRetriever, null).buildVersionFromKey("install_profile");

            final String[] versionIdParts = version.getId().split("-", 3);
            final boolean is1_12_2 = versionIdParts[0].equals("1.12.2");

            for (Library library : profileVersion.getLibrariesForOS()) {
                if (library.isForge()) {
                    // For Forge 1.12.2 > 2847, we have to inject the universal jar as a dependency
                    if (is1_12_2) {
                        library.setName(library.getName() + ":universal");
                        library.setUrl("https://files.minecraftforge.net/maven/");

                        // Add the mutated library
                        version.addLibrary(library);

                        checkLibraryQueue.addTask(new InstallVersionLibTask(library, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));
                    }

                    // We normally skip Forge because Forge specifies itself as a dependency, for some reason
                    continue;
                }

                checkLibraryQueue.addTask(new InstallVersionLibTask(library, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));
            }

            // For Forge 1.13+, we inject our ForgeWrapper as a dependency and launch it through that
            if (!is1_12_2) {
                Library forgeWrapper = new Library();
                // TODO: add hash validation
                forgeWrapper.setName("io.github.zekerzhayard:ForgeWrapper:1.5.1");

                version.addLibrary(forgeWrapper);

                version.setMainClass("io.github.zekerzhayard.forgewrapper.installer.Main");

                for (Library library : version.getLibrariesForOS()) {
                    if (library.isForge()) {
                        Library forgeLauncher = new Library();
                        forgeLauncher.setName(library.getName() + ":launcher");
                        forgeLauncher.setUrl("https://files.minecraftforge.net/maven/");

                        version.addLibrary(forgeLauncher);
                        checkLibraryQueue.addTask(new InstallVersionLibTask(forgeLauncher, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));

                        Library forgeUniversal = new Library();
                        forgeUniversal.setName(library.getName() + ":universal");
                        forgeUniversal.setUrl("https://files.minecraftforge.net/maven/");

                        checkLibraryQueue.addTask(new InstallVersionLibTask(forgeUniversal, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));

                        break;
                    }
                }
            }
        }

        for (Library library : version.getLibrariesForOS()) {
            // If minecraftforge is described in the libraries, skip it
            // HACK - Please let us get rid of this when we move to actually hosting forge,
            // or at least only do it if the users are sticking with modpack.jar
            if (library.isForge()) {
                continue;
            }

            if (isLegacy && library.getName().startsWith("net.minecraft:launchwrapper")) {
                continue;
            }

            // Log4j vulnerability patch - CVE-2021-44228
            // A hotfixed version of 2.0-beta9 for MC 1.7 - 1.12
            // And version 2.15.0 for >= 1.12
            if (library.isLog4j()) {
                final String[] libNameParts = library.getName().split(":");
                // org.apache.logging.log4j:log4j-core:2.0-beta9
                // org.apache.logging.log4j    log4j-core        2.0-beta9
                // Determine what version we need
                String log4jVersion = "2.15.0";
                if (libNameParts[2].equals("2.0-beta9")) {
                    log4jVersion = "2.0-beta9-fixed";
                }
                String sha1;
                int size;
                String artifactName = libNameParts[1];
                switch (log4jVersion) {
                    case "2.15.0":
                        switch (artifactName) {
                            case "log4j-api":
                                sha1 = "42319af9991a86b4475ab3316633a3d03e2d29e1";
                                size = 301805;
                                break;
                            case "log4j-core":
                                sha1 = "9bd89149d5083a2a3ab64dcc88b0227da14152ec";
                                size = 1789769;
                                break;
                            case "log4j-slf4j18-impl":
                                sha1 = "7c3f5758e86e1668929e907a5609a83671e21b30";
                                size = 21222;
                                break;
                            default:
                                throw new RuntimeException("Unknown log4j artifact " + artifactName + ", cannot continue");
                        }
                        break;
                    case "2.0-beta9-fixed":
                        switch (artifactName) {
                            case "log4j-api":
                                sha1 = "b61eaf2e64d8b0277e188262a8b771bbfa1502b3";
                                size = 107347;
                                break;
                            case "log4j-core":
                                sha1 = "677991ea2d7426f76309a73739cecf609679492c";
                                size = 677588;
                                break;
                            default:
                                throw new RuntimeException("Unknown log4j artifact " + artifactName + ", cannot continue");
                        }
                        break;
                    default:
                        throw new RuntimeException("Unknown log4j version " + log4jVersion + ", cannot continue");
                }
                String url = String.format("https://mirror.technicpack.net/Technic/lib/org/apache/logging/log4j/%1$s/%2$s/%1$s-%2$s.jar", artifactName, log4jVersion);
                Library fixedLog4j = new Library();
                fixedLog4j.setName("org.apache.logging.log4j:" + artifactName + ":" + log4jVersion);
                Artifact artifact = new Artifact(url, sha1, size);
                Downloads downloads = new Downloads();
                downloads.setArtifact(artifact);
                fixedLog4j.setDownloads(downloads);

                // Add fixed lib
                version.addLibrary(fixedLog4j);
                checkLibraryQueue.addTask(new InstallVersionLibTask(fixedLog4j, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));

                // Remove unpatched lib
                version.removeLibrary(library.getName());

                continue;
            }

            checkLibraryQueue.addTask(new InstallVersionLibTask(library, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));
        }

        queue.setMetadata(version);
    }
}
