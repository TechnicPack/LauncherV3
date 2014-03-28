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
import net.technicpack.launchercore.image.ISkinMapper;
import net.technicpack.minecraftcore.LauncherDirectories;

import java.awt.image.BufferedImage;
import java.io.File;

public class TechnicSkinMapper implements ISkinMapper {
    private LauncherDirectories directories;
    private ResourceLoader resources;

    public TechnicSkinMapper(LauncherDirectories directories) {
        this.directories = directories;
    }

    @Override
    public String getSkinFilename(User user) {
        return directories.getAssetsDirectory() + File.separator + "skins" + File.separator + user.getDisplayName() + ".png";
    }

    @Override
    public String getFaceFilename(User user) {
        return directories.getAssetsDirectory() + File.separator + "avatars" + File.separator + user.getDisplayName() + ".png";
    }

    @Override
    public BufferedImage getDefaultSkinImage() {
        return null;
    }

    @Override
    public BufferedImage getDefaultFaceImage() {
        return resources.getImage("news/authorHelm.png");
    }
}
