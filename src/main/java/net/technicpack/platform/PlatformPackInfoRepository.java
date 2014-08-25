/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.platform;

import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.utilslib.Utils;

import java.util.logging.Level;

public class PlatformPackInfoRepository implements IAuthoritativePackSource {
    private IPlatformApi platform;
    private ISolderApi solder;

    public PlatformPackInfoRepository(IPlatformApi platform, ISolderApi solder) {
        this.platform = platform;
        this.solder = solder;
    }

    @Override
    public PackInfo getPackInfo(InstalledPack pack) {
        if (!pack.isPlatform())
            return null;

        return getPlatformPackInfo(pack.getName());
    }

    @Override
    public PackInfo getCompletePackInfo(PackInfo pack) {
        return getPlatformPackInfo(pack.getName());
    }

    protected PackInfo getPlatformPackInfo(String slug) {
        try {
            PackInfo info = null;

            PlatformPackInfo platformInfo = platform.getPlatformPackInfo(slug);

            if (platformInfo != null && platformInfo.hasSolder()) {
                ISolderPackApi solderPack = solder.getSolderPack(platformInfo.getSolder(), platformInfo.getName());
                info = solderPack.getPackInfo();
            } else {
                info = platformInfo;
            }

            return info;
        } catch (RestfulAPIException ex) {
            Utils.getLogger().log(Level.WARNING, "Unable to load platform pack " + slug, ex);
            return null;
        }
    }
}
