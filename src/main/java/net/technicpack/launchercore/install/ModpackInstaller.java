/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.utilslib.Utils;

import java.io.IOException;

public class ModpackInstaller<VersionData> {
    private final IPlatformApi platformApi;
    private final String clientId;

    public ModpackInstaller(IPlatformApi platformApi, String clientId) {
        this.clientId = clientId;
        this.platformApi = platformApi;
    }

    public VersionData installPack(InstallTasksQueue<VersionData> tasksQueue, ModpackModel modpack, String build) throws IOException, InterruptedException {
        modpack.save();
        modpack.initDirectories();

        Version installedVersion = modpack.getInstalledVersion();
        tasksQueue.runAllTasks();

        Version versionFile = new Version(build, false);
        versionFile.save(modpack.getBinDir());

        if (installedVersion == null) {
            platformApi.incrementPackInstalls(modpack.getName());
            Utils.sendTracking("installModpack", modpack.getName(), modpack.getBuild(), clientId);
        }

        return tasksQueue.getMetadata();
    }
}
