/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.io;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.image.IImageMapper;
import net.technicpack.minecraftcore.LauncherDirectories;

import java.awt.image.BufferedImage;
import java.io.File;

public class TechnicFaceMapper implements IImageMapper<User> {
    private LauncherDirectories directories;
    private ResourceLoader resources;
    private BufferedImage defaultImage;

    public TechnicFaceMapper(LauncherDirectories directories, ResourceLoader resources) {
        this.directories = directories;
        this.resources = resources;
        defaultImage = resources.getImage("news/authorHelm.png");
    }

    @Override
    public boolean shouldDownloadImage(User imageKey) {
        return true;
    }

    @Override
    public File getImageLocation(User imageKey) {
        return new File(directories.getAssetsDirectory(), "avatars" + File.separator + imageKey.getDisplayName() + ".png");
    }

    @Override
    public BufferedImage getDefaultImage() {
        return defaultImage;
    }
}
