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
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class MoveLauncherPackage implements IInstallTask {
    private String description;
    private File launcher;
    private Relauncher relauncher;

    public MoveLauncherPackage(String description, File launcher, Relauncher relauncher) {
        this.description = description;
        this.launcher = launcher;
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
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        String currentPath = relauncher.getRunningPath();
        Utils.getLogger().log(Level.INFO, "Moving running package from " + currentPath + " to " + launcher.getAbsolutePath());

        File source = new File(currentPath);
        File dest = new File(launcher.getAbsolutePath());

        if (!source.equals(dest)) {
            if (dest.exists()) {
                if (!dest.delete())
                    Utils.getLogger().log(Level.SEVERE, "Deletion of existing package failed!");
            }
            FileInputStream sourceStream = null;
            FileOutputStream destStream = null;

            try {
                if (!dest.getParentFile().exists())
                    dest.getParentFile().mkdirs();
                dest.createNewFile();
                sourceStream = new FileInputStream(source);
                destStream = new FileOutputStream(dest);
                IOUtils.copy(sourceStream, destStream);
            } catch (IOException ex) {
                Utils.getLogger().log(Level.SEVERE, "Error attempting to copy download package:", ex);
            } finally {
                IOUtils.closeQuietly(sourceStream);
                IOUtils.closeQuietly(destStream);
            }
        }

        dest.setExecutable(true, true);
    }
}
