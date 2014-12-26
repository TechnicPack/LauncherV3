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

package net.technicpack.solder.cache;

import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.Collection;

public class CachedSolderApi implements ISolderApi {

    private ISolderApi innerApi;
    private Collection<SolderPackInfo> cachedPublicPacks = null;
    private DateTime lastSolderPull = new DateTime(0);
    private int cacheInSeconds;

    public CachedSolderApi(ISolderApi innerApi, int cacheInSeconds) {
        this.innerApi = innerApi;
        this.cacheInSeconds = cacheInSeconds;
    }

    @Override
    public ISolderPackApi getSolderPack(String solderRoot, String modpackSlug) throws RestfulAPIException {
        return new CachedSolderPackApi(innerApi.getSolderPack(solderRoot, modpackSlug), cacheInSeconds);
    }

    @Override
    public Collection<SolderPackInfo> getPublicSolderPacks(String solderRoot) throws RestfulAPIException {
        if (Seconds.secondsBetween(DateTime.now(), lastSolderPull).isLessThan(Seconds.seconds(cacheInSeconds))) {
            if (cachedPublicPacks != null)
                return cachedPublicPacks;
        }

        if (Seconds.secondsBetween(DateTime.now(), lastSolderPull).isLessThan(Seconds.seconds(cacheInSeconds/10)))
            return new ArrayList<SolderPackInfo>(0);

        try {
            cachedPublicPacks = innerApi.getPublicSolderPacks(solderRoot);
            return cachedPublicPacks;
        } finally {
            lastSolderPull = DateTime.now();
        }
    }
}
