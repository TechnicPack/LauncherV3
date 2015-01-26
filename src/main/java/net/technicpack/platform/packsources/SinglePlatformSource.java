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

package net.technicpack.platform.packsources;

import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.ISolderApi;

import java.util.ArrayList;
import java.util.Collection;

public class SinglePlatformSource extends PlatformPackInfoRepository implements IPackSource {
    private String platformUrl;

    public SinglePlatformSource(IPlatformApi platformApi, ISolderApi solderApi, String platformUrl) {
        super(platformApi, solderApi);
        this.platformUrl = platformUrl;
    }

    @Override
    public String getSourceName() {
        return "Platform pack from '" + this.platformUrl + "'";
    }

    @Override
    public Collection<PackInfo> getPublicPacks() {
        ArrayList<PackInfo> packs = new ArrayList<PackInfo>(1);
        try {
            PlatformPackInfo info = RestObject.getRestObject(PlatformPackInfo.class, platformUrl.toString());

            if (info != null) {
                packs.add(getInfoFromPlatformInfo(info));
            }
        } catch (RestfulAPIException ex) {
            //The url was garbage for whatever reason so just don't return anything
        }

        return packs;
    }

    @Override
    public int getPriority(PackInfo packInfo) {
        return 0;
    }

    @Override
    public boolean isOfficialPack(String packSlug) {
        return false;
    }
}
