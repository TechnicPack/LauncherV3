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

package net.technicpack.solder.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class CachedSolderApi implements ISolderApi {

    private LauncherDirectories directories;
    private ISolderApi innerApi;
    private Collection<SolderPackInfo> cachedPublicPacks = null;
    private DateTime lastSolderPull = new DateTime(0);
    private int cacheInSeconds;

    private class CacheTuple {
        private String root;
        private String slug;
        private String url;

        public CacheTuple(String root, String slug, String url) {
            this.root = root;
            this.slug = slug;
            this.url = url;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof CacheTuple))
                return false;

            if (!root.equals(((CacheTuple) obj).root))
                return false;
            if (!slug.equals(((CacheTuple) obj).slug))
                return false;
            if (!url.equals(((CacheTuple) obj).url))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            int hash = root.hashCode();
            hash *= 31;
            hash += slug.hashCode();
            hash *= 31;
            hash += url.hashCode();
            return hash;
        }
    }

    private Cache<CacheTuple, ISolderPackApi> packs;

    public CachedSolderApi(LauncherDirectories directories, ISolderApi innerApi, int cacheInSeconds) {
        this.directories = directories;
        this.innerApi = innerApi;
        this.cacheInSeconds = cacheInSeconds;

        packs = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(50)
                .expireAfterWrite(cacheInSeconds, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public ISolderPackApi getSolderPack(String solderRoot, String modpackSlug, String mirrorUrl) throws RestfulAPIException {
        CacheTuple tuple = new CacheTuple(solderRoot, modpackSlug, mirrorUrl);
        ISolderPackApi pack = packs.getIfPresent(tuple);

        if (pack == null) {
            pack = new CachedSolderPackApi(directories, innerApi.getSolderPack(solderRoot, modpackSlug, mirrorUrl), cacheInSeconds, modpackSlug);
            packs.put(tuple, pack);
        }

        return pack;
    }

    @Override
    public Collection<SolderPackInfo> getPublicSolderPacks(String solderRoot) throws RestfulAPIException {
        return internalGetPublicSolderPacks(solderRoot, this);
    }

    @Override
    public Collection<SolderPackInfo> internalGetPublicSolderPacks(String solderRoot, ISolderApi packFactory) throws RestfulAPIException {
        if (Seconds.secondsBetween(lastSolderPull, DateTime.now()).isLessThan(Seconds.seconds(cacheInSeconds))) {
            if (cachedPublicPacks != null)
                return cachedPublicPacks;
        }

        if (Seconds.secondsBetween(lastSolderPull, DateTime.now()).isLessThan(Seconds.seconds(cacheInSeconds / 10)))
            return new ArrayList<SolderPackInfo>(0);

        try {
            cachedPublicPacks = innerApi.internalGetPublicSolderPacks(solderRoot, this);
            return cachedPublicPacks;
        } finally {
            lastSolderPull = DateTime.now();
        }
    }

    @Override
    public String getMirrorUrl(String solderRoot) throws RestfulAPIException {
        return innerApi.getMirrorUrl(solderRoot);
    }
}
