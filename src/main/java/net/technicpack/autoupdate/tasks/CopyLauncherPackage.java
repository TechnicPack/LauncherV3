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

package net.technicpack.autoupdate.tasks;

import net.technicpack.autoupdate.Relauncher;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class CopyLauncherPackage implements IInstallTask<Void> {
    private String description;
    private File targetFile;
    private Relauncher relauncher;

    public CopyLauncherPackage(String description, File targetFile, Relauncher relauncher) {
        this.description = description;
        this.targetFile = targetFile;
        this.relauncher = relauncher;
    }

    @Override
    public String getTaskDescription() {
        return description;
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<Void> queue) throws IOException {
        Path currentPath = Paths.get(relauncher.getRunningPath());
        Path targetPath = Paths.get(targetFile.getAbsolutePath());

        Utils.getLogger().log(Level.INFO, String.format("Copying running package from %s to %s", currentPath, targetPath));

        if (currentPath.equals(targetPath)) {
            throw new IOException("Source and destination paths are the same!");
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            // TODO: wrap the IOException
            Utils.getLogger().log(Level.SEVERE, "Failed to delete the existing target package", e);
            throw e;
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(currentPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Error copying package", e);
            throw e;
        }

        if (!targetFile.setExecutable(true, true)) {
            Utils.getLogger().log(Level.WARNING, "Failed to set executable flag on package");
        }
    }
}
