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

import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.FileVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.ZipFileRetriever;
import net.technicpack.minecraftcore.mojang.version.io.Artifact;
import net.technicpack.minecraftcore.mojang.version.io.Downloads;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HandleVersionFileTask implements IInstallTask<MojangVersion> {
    // Taken from https://stackoverflow.com/a/27872852
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private ModpackModel pack;
    private LauncherFileSystem fileSystem;
    private ITasksQueue<MojangVersion> checkLibraryQueue;
    private ITasksQueue<MojangVersion> downloadLibraryQueue;
    private ITasksQueue<MojangVersion> copyLibraryQueue;
    private ITasksQueue<MojangVersion> checkNonMavenLibsQueue;
    private MojangVersionBuilder versionBuilder;
    private ILaunchOptions launchOptions;
    private IJavaRuntime javaRuntime;

    private String libraryName;

    public HandleVersionFileTask withPack(ModpackModel pack) {
        this.pack = pack;
        return this;
    }

    public HandleVersionFileTask withFileSystem(LauncherFileSystem fileSystem) {
        this.fileSystem = fileSystem;
        return this;
    }

    public HandleVersionFileTask withCheckLibraryQueue(ITasksQueue<MojangVersion> checkLibraryQueue) {
        this.checkLibraryQueue = checkLibraryQueue;
        return this;
    }

    public HandleVersionFileTask withDownloadLibraryQueue(ITasksQueue<MojangVersion> downloadLibraryQueue) {
        this.downloadLibraryQueue = downloadLibraryQueue;
        return this;
    }

    public HandleVersionFileTask withCopyLibraryQueue(ITasksQueue<MojangVersion> copyLibraryQueue) {
        this.copyLibraryQueue = copyLibraryQueue;
        return this;
    }

    public HandleVersionFileTask withCheckNonMavenLibsQueue(ITasksQueue<MojangVersion> checkNonMavenLibsQueue) {
        this.checkNonMavenLibsQueue = checkNonMavenLibsQueue;
        return this;
    }

    public HandleVersionFileTask withVersionBuilder(MojangVersionBuilder versionBuilder) {
        this.versionBuilder = versionBuilder;
        return this;
    }

    public HandleVersionFileTask withLaunchOptions(ILaunchOptions launchOptions) {
        this.launchOptions = launchOptions;
        return this;
    }

    public HandleVersionFileTask withJavaRuntime(IJavaRuntime javaRuntime) {
        this.javaRuntime = javaRuntime;
        return this;
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
    public void runTask(InstallTasksQueue<MojangVersion> queue) throws IOException, InterruptedException {
        Objects.requireNonNull(pack, "ModpackModel must be set.");
        Objects.requireNonNull(fileSystem, "LauncherFileSystem must be set.");
        Objects.requireNonNull(checkLibraryQueue, "CheckLibraryQueue must be set.");
        Objects.requireNonNull(downloadLibraryQueue, "DownloadLibraryQueue must be set.");
        Objects.requireNonNull(copyLibraryQueue, "CopyLibraryQueue must be set.");
        Objects.requireNonNull(checkNonMavenLibsQueue, "CheckNonMavenLibsQueue must be set.");
        Objects.requireNonNull(versionBuilder, "VersionBuilder must be set.");
        Objects.requireNonNull(launchOptions, "LaunchOptions must be set.");
        Objects.requireNonNull(javaRuntime, "JavaRuntime must be set.");

        MojangVersion version = versionBuilder.buildVersionFromKey(null);

        if (version == null) {
            throw new DownloadException("The version.json file was invalid.");
        }

        version.setJavaRuntime(javaRuntime);

        // if MC < 1.6, we inject LegacyWrapper
        // HACK
        boolean isLegacy = MojangUtils.isLegacyVersion(version.getParentVersion());

        if (isLegacy) {
            Library legacyWrapper = new Library(
                    "net.technicpack:legacywrapper:1.2.1",
                    TechnicConstants.TECHNIC_LIB_REPO + "net/technicpack/legacywrapper/1.2.1/legacywrapper-1.2.1.jar",
                    "741cbc946421a5a59188a51108e1ce5cb5674681",
                    77327
            );

            version.addLibrary(legacyWrapper);

            version.setMainClass("net.technicpack.legacywrapper.Launch");
        }

        final boolean hasNeoForge = MojangUtils.hasNeoForge(version);

        // In Forge 1.13+ and 1.12.2 > 2847, there's an installer jar, and the universal jar can't be used since it
        // doesn't have the required dependencies to actually launch the game.
        // So, for Forge 1.13+ we need to use ForgeWrapper to install Forge (it performs deobfuscation at install time).
        // For Forge 1.12.2 it launches directly, as long as we inject the universal jar (it won't launch at all if
        // you try running it through ForgeWrapper).
        final boolean hasModernMinecraftForge = MojangUtils.hasModernMinecraftForge(version);

        if (hasModernMinecraftForge || hasNeoForge) {
            File profileJson = new File(pack.getBinDir(), "install_profile.json");
            ZipFileRetriever zipVersionRetriever = new ZipFileRetriever(new File(pack.getBinDir(), "modpack.jar"));
            MojangVersion installerVersion = new FileVersionBuilder(profileJson, zipVersionRetriever, null).buildVersionFromKey("install_profile");

            // These are for Minecraft Forge. They're invalid (but safe) with NeoForge
            final String[] versionIdParts = version.getId().split("-", 3);
            final boolean is1_12_2 = versionIdParts[0].equals("1.12.2");

            if (is1_12_2) {
                for (Library library : version.getLibrariesForCurrentOS(launchOptions, javaRuntime)) {
                    // For Minecraft Forge 1.12.2 > 2847, we correct the classifier for the library to "universal".
                    // The Forge installer just grabs this jar from within itself ("maven" folder), but we grab it
                    // directly from the Minecraft Forge Maven repo.
                    // The Minecraft Forge universal jar is already specified in the version.json but doesn't have the
                    // "universal" classifier, so we change the name to add the "universal" classifier (for download
                    // purposes), generate and set the artifact url, then change the name back to not have a
                    // classifier, effectively downloading it to the correct path and having it there ready
                    // for launch without any modifications necessary to the classpath generation process
                    // or version.json file.
                    // This library is being modified in-place
                    if (library.getGradleGroup().equals("net.minecraftforge")
                        && library.getGradleArtifact().equals("forge")
                        && (library.getGradleClassifier() == null || library.getGradleClassifier().isEmpty())) {
                        // Correct the classifier to "universal"
                        String oldName = library.getName();
                        library.setName(library.getName() + ":universal");

                        // Set the download URL for the library
                        Downloads downloads = library.getDownloads();
                        Artifact artifact = downloads.getArtifact();
                        artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());

                        // Revert the classifier change
                        library.setName(oldName);

                        break;
                    }
                }
            }

            // Process the install_profile.json (installer) libraries
            List<Library> dedupedInstallerLibraries = installerVersion.getLibraries().stream().filter(distinctByKey(Library::getName)).collect(Collectors.toList());

            for (Library library : dedupedInstallerLibraries) {
                if (library.isMinecraftForge() && is1_12_2) {
                    // Handling of the modern 1.12.2 Minecraft Forge universal jar is done above, in the version.json, rather than the install_profile.json
                    continue;
                }

                // For Minecraft Forge 1.13+ up to 1.17, the URL for the universal jar isn't set, so we set one here
                // For Minecraft Forge 1.17+ this has no effect, since the URL is already set
                // For NeoForge this has no effect, since it uses the "net.neoforged" group
                if (library.getGradleGroup().equals("net.minecraftforge")
                        && library.getGradleArtifact().equals("forge")
                        && library.getGradleClassifier() != null
                        && library.getGradleClassifier().equals("universal")
                        && !is1_12_2) {
                    Downloads downloads = library.getDownloads();
                    Artifact artifact = downloads.getArtifact();

                    if (artifact.getUrl() == null || artifact.getUrl().isEmpty()) {
                        // Modify the URL in-place, the altered library will get added by the rest of the logic
                        artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());
                    }
                }

                checkLibraryQueue.addTask(new InstallVersionLibTask(library, checkNonMavenLibsQueue,
                                                                    downloadLibraryQueue, copyLibraryQueue, pack, fileSystem));
            }

            // For Minecraft Forge 1.13+ and NeoForge, we inject our ForgeWrapper as a dependency and launch it through that
            if (!is1_12_2) {
                Library forgeWrapper = new Library(
                        "io.github.zekerzhayard:ForgeWrapper:1.6.0-technic",
                        TechnicConstants.TECHNIC_LIB_REPO + "io/github/zekerzhayard/ForgeWrapper/1.6.0-technic/ForgeWrapper-1.6.0-technic.jar",
                        "8764cbf4c7ded7ac0ad9136a0070bbfeee8813cf",
                        34944
                );

                version.prependLibrary(forgeWrapper);

                version.setMainClass("io.github.zekerzhayard.forgewrapper.installer.Main");

                for (Library library : version.getLibrariesForCurrentOS(launchOptions, javaRuntime)) {
                    // This is for Minecraft Forge 1.13+, up to 1.17 (not inclusive)
                    // For NeoForge this has no effect, since it uses the "net.neoforged" group
                    if (library.getGradleGroup().equals("net.minecraftforge")
                            && library.getGradleArtifact().equals("forge")
                            && (library.getGradleClassifier() == null || library.getGradleClassifier().isEmpty())
                    ) {
                        // Correct the classifier to "launcher" for the download
                        String oldName = library.getName();
                        library.setName(library.getName() + ":launcher");

                        // Set the download URL for the library
                        Downloads downloads = library.getDownloads();
                        Artifact artifact = downloads.getArtifact();

                        if (artifact.getUrl() == null || artifact.getUrl().isEmpty()) {
                            // Modify the URL in-place, the altered library will get added by the rest of the logic
                            artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());
                        }

                        // Revert the classifier change
                        library.setName(oldName);

                        break;
                    }

                    // Minecraft Forge 49.0.4+ sets a "client" library (I presume it's for the finalized MC jar),
                    // so we remove it here since we're using ForgeWrapper
                    if (library.getGradleGroup().equals("net.minecraftforge")
                            && library.getGradleArtifact().equals("forge")
                            && library.getGradleClassifier() != null
                            && library.getGradleClassifier().equals("client")) {
                        version.removeLibrary(library.getName());
                    }
                }
            }
        }

        for (Library library : version.getLibrariesForCurrentOS(launchOptions, javaRuntime)) {
            // Skip the Minecraft Forge jar if not using modern Minecraft Forge,
            // since it will be loaded via modpack.jar later on
            if (library.isMinecraftForge() && !hasModernMinecraftForge) {
                version.removeLibrary(library.getName());
                continue;
            }

            // Remove the vanilla launchwrapper, since we use our own with some modifications
            if (isLegacy && library.getName().startsWith("net.minecraft:launchwrapper:")) {
                version.removeLibrary(library.getName());
                continue;
            }

            // Log4j vulnerability patch - CVE-2021-44228 <https://nvd.nist.gov/vuln/detail/CVE-2021-44228>
            // A hotfixed version of 2.0-beta9 for MC 1.7 - 1.12
            // And version 2.16.0 for >= 1.12
            // If log4j is at least 2.16.0, we have nothing to do here, since it isn't vulnerable
            if (library.isLog4j() && (new ComparableVersion(library.getGradleVersion())).compareTo(new ComparableVersion("2.16.0")) < 0) {
                final String[] libNameParts = library.getName().split(":");
                // org.apache.logging.log4j:log4j-core:2.0-beta9
                // org.apache.logging.log4j    log4j-core        2.0-beta9
                // Determine what version we need
                String log4jVersion = "2.16.0";
                if (libNameParts[2].equals("2.0-beta9")) {
                    log4jVersion = "2.0-beta9-fixed";
                }
                String sha1;
                int size;
                String artifactName = libNameParts[1];
                switch (log4jVersion) {
                    case "2.16.0":
                        switch (artifactName) {
                            case "log4j-api":
                                sha1 = "f821a18687126c2e2f227038f540e7953ad2cc8c";
                                size = 301892;
                                break;
                            case "log4j-core":
                                sha1 = "539a445388aee52108700f26d9644989e7916e7c";
                                size = 1789565;
                                break;
                            case "log4j-slf4j18-impl":
                                sha1 = "0c880a059056df5725f5d8d1035276d9749eba6d";
                                size = 21249;
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
                String url = String.format(TechnicConstants.TECHNIC_LIB_REPO + "org/apache/logging/log4j/%1$s/%2$s/%1$s-%2$s.jar", artifactName, log4jVersion);
                Library fixedLog4j = new Library(
                        "org.apache.logging.log4j:" + artifactName + ":" + log4jVersion,
                        url,
                        sha1,
                        size
                );

                // Add fixed lib
                version.addLibrary(fixedLog4j);
                checkLibraryQueue.addTask(new InstallVersionLibTask(fixedLog4j, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, fileSystem));

                // Remove unpatched lib
                version.removeLibrary(library.getName());

                continue;
            }

            checkLibraryQueue.addTask(new InstallVersionLibTask(library, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, fileSystem));
        }

        queue.setMetadata(version);
    }
}
