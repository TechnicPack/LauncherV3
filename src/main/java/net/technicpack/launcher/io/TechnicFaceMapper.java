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

import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.launchercore.image.IImageMapper;
import net.technicpack.launchercore.install.LauncherDirectories;

import java.awt.image.BufferedImage;
import java.io.File;

public class TechnicFaceMapper implements IImageMapper<IUserType> {
    private LauncherDirectories directories;
    private BufferedImage defaultImage;

    public TechnicFaceMapper(LauncherDirectories directories, ResourceLoader resources) {
        this.directories = directories;
        defaultImage = resources.getImage("news/authorHelm.png");
    }

    @Override
    public boolean shouldDownloadImage(IUserType imageKey) {
        return true;
    }

    @Override
    public File getImageLocation(IUserType imageKey) {
        return new File(directories.getAssetsDirectory(), "avatars" + File.separator + imageKey.getDisplayName() + ".png");
    }

    @Override
    public BufferedImage getDefaultImage() {
        return defaultImage;
    }
}
