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

package net.technicpack.launchercore.modpacks.resources;

import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.image.IImageMapper;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IModpackResourceType;
import net.technicpack.rest.io.Resource;
import net.technicpack.utilslib.CryptoUtils;

import java.awt.image.BufferedImage;
import java.io.File;

public class PackResourceMapper implements IImageMapper<ModpackModel> {

    private LauncherFileSystem fileSystem;
    private BufferedImage defaultImage;
    private IModpackResourceType resourceType;

    public PackResourceMapper(LauncherFileSystem fileSystem, BufferedImage defaultImage, IModpackResourceType resourceType) {
        this.fileSystem = fileSystem;
        this.defaultImage = defaultImage;
        this.resourceType = resourceType;
    }

    @Override
    public boolean shouldDownloadImage(ModpackModel imageKey) {
        Resource res = resourceType.getResource(imageKey);

        if (res == null) {
            return false;
        }

        final String md5 = res.getMd5();

        if (md5 == null || md5.isEmpty()) {
            return true;
        }

        return !CryptoUtils.checkMD5(getImageLocation(imageKey), md5);
    }

    @Override
    public File getImageLocation(ModpackModel imageKey) {
        File assets = new File(fileSystem.getAssetsDirectory(), "packs");
        File packs = new File(assets, imageKey.getName());
        return new File(packs, resourceType.getImageName());
    }

    @Override
    public BufferedImage getDefaultImage() {
        return defaultImage;
    }
}
