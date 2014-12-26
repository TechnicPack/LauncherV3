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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.concurrent.TimeUnit;

public class CachedSolderPackApi implements ISolderPackApi {

    private ISolderPackApi innerApi;
    private int cacheInSeconds;

    private SolderPackInfo rootInfoCache = null;
    private DateTime lastInfoAccess = new DateTime(0);

    private Cache<String, Modpack> buildCache;
    private Cache<String, Boolean> deadBuildCache;

    public CachedSolderPackApi(ISolderPackApi innerApi, int cacheInSeconds) {
        this.innerApi = innerApi;
        this.cacheInSeconds = cacheInSeconds;

        buildCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds, TimeUnit.SECONDS)
                .build();

        deadBuildCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds/10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getMirrorUrl() {
        return innerApi.getMirrorUrl();
    }

    @Override
    public SolderPackInfo getPackInfoForBulk() throws RestfulAPIException {
        return getPackInfo();
    }

    @Override
    public SolderPackInfo getPackInfo() throws RestfulAPIException {
        if (Seconds.secondsBetween(DateTime.now(), lastInfoAccess).isGreaterThan(Seconds.seconds(cacheInSeconds))) {
            if (rootInfoCache != null)
                return rootInfoCache;
        }

        if (Seconds.secondsBetween(DateTime.now(), lastInfoAccess).isGreaterThan(Seconds.seconds(cacheInSeconds/10)))
            return rootInfoCache;

        try {
            rootInfoCache = innerApi.getPackInfoForBulk();
            return rootInfoCache;
        } finally {
            lastInfoAccess = DateTime.now();
        }
    }

    @Override
    public Modpack getPackBuild(String build) throws BuildInaccessibleException {

        Boolean isDead = deadBuildCache.getIfPresent(build);

        if (isDead != null && isDead.booleanValue())
            return null;

        Modpack modpack = buildCache.getIfPresent(build);

        if (modpack != null) {
            return modpack;
        }

        try {
            modpack = innerApi.getPackBuild(build);

            if (modpack != null) {
                buildCache.put(build, modpack);
            }

            return modpack;
        } finally {
            deadBuildCache.put(build, modpack == null);
        }
    }
}
