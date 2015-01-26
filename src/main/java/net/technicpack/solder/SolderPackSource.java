/**
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

package net.technicpack.solder;

import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

public class SolderPackSource implements IPackSource {
    private String baseUrl;
    private ISolderApi solder;
    private boolean isOfficialSource;

    public SolderPackSource(String baseUrl, ISolderApi solder, boolean isOfficialSource) {
        this.baseUrl = baseUrl;
        this.solder = solder;
        this.isOfficialSource = isOfficialSource;
    }

    @Override
    public String getSourceName() {
        return "Public packs for solder " + baseUrl;
    }

    @Override
    public Collection<PackInfo> getPublicPacks() {
        LinkedList<PackInfo> returnValue = new LinkedList<PackInfo>();

        try {
            Collection<SolderPackInfo> packs = solder.getPublicSolderPacks(baseUrl);

            for (SolderPackInfo info : packs) {
                returnValue.add(info);
            }
        } catch (RestfulAPIException ex) {
            Utils.getLogger().log(Level.WARNING, "Unable to load technic modpacks", ex);
        }

        return returnValue;
    }

    @Override
    public int getPriority(PackInfo pack) {
        return 5000;
    }

    @Override
    public boolean isOfficialPack(String slug) {
        return isOfficialSource;
    }
}
