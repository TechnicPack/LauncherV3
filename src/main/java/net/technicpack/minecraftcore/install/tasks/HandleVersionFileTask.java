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
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.utilslib.maven.MavenConnector;

import java.io.IOException;

public class HandleVersionFileTask implements IInstallTask {
    private final ModpackModel pack;
    private final LauncherDirectories directories;
    private final ITasksQueue checkLibraryQueue;
    private final ITasksQueue downloadLibraryQueue;
    private final ITasksQueue copyLibraryQueue;
    private final ITasksQueue checkNonMavenLibsQueue;
    private final MojangVersionBuilder versionBuilder;
    private final MavenConnector mavenConnector;

    private String libraryName;

    public HandleVersionFileTask(ModpackModel pack, LauncherDirectories directories, ITasksQueue checkNonMavenLibsQueue, ITasksQueue checkLibraryQueue, ITasksQueue downloadLibraryQueue, ITasksQueue copyLibraryQueue, MojangVersionBuilder versionBuilder) {
        this.pack = pack;
        this.directories = directories;
        this.checkLibraryQueue = checkLibraryQueue;
        this.downloadLibraryQueue = downloadLibraryQueue;
        this.copyLibraryQueue = copyLibraryQueue;
        this.checkNonMavenLibsQueue = checkNonMavenLibsQueue;
        this.versionBuilder = versionBuilder;
        this.mavenConnector = new MavenConnector(directories, "forge", "http://files.minecraftforge.net/maven/");
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

        for (Library library : version.getLibrariesForOS()) {
            // If minecraftforge is described in the libraries, skip it
            // HACK - Please let us get rid of this when we move to actually hosting forge,
            // or at least only do it if the users are sticking with modpack.jar
            if (library.getName().startsWith("net.minecraftforge:minecraftforge") ||
                    library.getName().startsWith("net.minecraftforge:forge")) {
                continue;
            }

            checkLibraryQueue.addTask(new InstallVersionLibTask(library, mavenConnector, checkNonMavenLibsQueue, downloadLibraryQueue, copyLibraryQueue, pack, directories));
        }

        queue.setMetadata(version);
    }
}
