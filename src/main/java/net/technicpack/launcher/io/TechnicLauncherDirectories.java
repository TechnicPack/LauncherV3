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

import net.technicpack.launchercore.install.LauncherDirectories;

import java.io.File;

public class TechnicLauncherDirectories extends LauncherDirectories {
    private File workDir;

    public TechnicLauncherDirectories(File rootDir) {
        workDir = rootDir;
    }

    public File getLauncherDirectory() {
        if (!workDir.exists())
            workDir.mkdirs();

        return workDir;
    }

    public File getCacheDirectory() {
        File cache = new File(getLauncherDirectory(), "cache");
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }

    public File getAssetsDirectory() {
        File assets = new File(getLauncherDirectory(), "assets");

        if (!assets.exists()) {
            assets.mkdirs();
        }

        return assets;
    }

    public File getModpacksDirectory() {
        File modpacks = new File(getLauncherDirectory(), "modpacks");

        if (!modpacks.exists())
            modpacks.mkdirs();

        return modpacks;
    }
}