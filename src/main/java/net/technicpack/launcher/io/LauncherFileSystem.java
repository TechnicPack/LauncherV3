/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.io;

import javax.swing.JOptionPane;
import java.io.File;

public class LauncherFileSystem {
    private File workDir;

    public LauncherFileSystem(File rootDir) {
        // TODO: this constructor should create the folders and ensure they exist or abort
        workDir = rootDir;
    }

    private void ensureDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            return;
        }

        if (dir.exists() && !dir.isDirectory()) {
            if (!dir.delete()) {
                JOptionPane.showMessageDialog(null, "Failed to create directory " + dir.getAbsolutePath() + ".\nThis is a critical error, the launcher will terminate now.", "Critical error - Technic Launcher", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        if (!dir.mkdirs()) {
            JOptionPane.showMessageDialog(null, "Failed to create directory " + dir.getAbsolutePath() + ".\nThis is a critical error, the launcher will terminate now.", "Critical error - Technic Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public File getLauncherDirectory() {
        ensureDirectory(workDir);

        return workDir;
    }

    public File getCacheDirectory() {
        File cache = new File(getLauncherDirectory(), "cache");

        ensureDirectory(cache);

        return cache;
    }

    public File getAssetsDirectory() {
        // TODO: all of these directories should be final and just one instance, it's stupid to make a new instance every single time
        File assets = new File(getLauncherDirectory(), "assets");

        ensureDirectory(assets);

        return assets;
    }

    public File getModpacksDirectory() {
        File modpacks = new File(getLauncherDirectory(), "modpacks");

        ensureDirectory(modpacks);

        return modpacks;
    }

    public File getRuntimesDirectory() {
        File runtimes = new File(getLauncherDirectory(), "runtimes");

        ensureDirectory(runtimes);

        return runtimes;
    }

    public File getLogsDirectory() {
        File logs = new File(getLauncherDirectory(), "logs");

        ensureDirectory(logs);

        return logs;
    }
}
