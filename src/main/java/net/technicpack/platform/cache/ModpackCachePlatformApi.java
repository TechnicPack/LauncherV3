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

package net.technicpack.platform.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.NewsData;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.platform.io.SearchResultsData;
import net.technicpack.rest.RestfulAPIException;

import java.util.concurrent.TimeUnit;

public class ModpackCachePlatformApi implements IPlatformApi {

    private IPlatformApi innerApi;
    Cache<String, PlatformPackInfo> cache;
    Cache<String, Boolean> deadPacks;

    public ModpackCachePlatformApi(IPlatformApi innerApi, int cacheInSeconds) {
        this.innerApi = innerApi;
        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds, TimeUnit.SECONDS)
                .build();

        deadPacks = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds/10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public PlatformPackInfo getPlatformPackInfoForBulk(String packSlug) throws RestfulAPIException {
        return getPlatformPackInfo(packSlug);
    }

    @Override
    public PlatformPackInfo getPlatformPackInfo(String packSlug) throws RestfulAPIException {
        Boolean isDead = deadPacks.getIfPresent(packSlug);

        if (isDead != null && isDead.booleanValue())
            return null;

        PlatformPackInfo info = cache.getIfPresent(packSlug);

        try {
            if (info == null) {
                info = innerApi.getPlatformPackInfoForBulk(packSlug);
                cache.put(packSlug, info);
            }
        } finally {
            deadPacks.put(packSlug, info == null);
        }

        return info;
    }

    @Override
    public String getPlatformUri(String packSlug) {
        return innerApi.getPlatformUri(packSlug);
    }

    @Override
    public void incrementPackRuns(String packSlug) {
        innerApi.incrementPackRuns(packSlug);
    }

    @Override
    public void incrementPackInstalls(String packSlug) {
        innerApi.incrementPackInstalls(packSlug);
    }

    @Override
    public NewsData getNews() throws RestfulAPIException {
        return innerApi.getNews();
    }

    @Override
    public SearchResultsData getSearchResults(String searchTerm) throws RestfulAPIException {
        return innerApi.getSearchResults(searchTerm);
    }
}
