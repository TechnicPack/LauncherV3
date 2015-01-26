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

package net.technicpack.platform.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.NewsData;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.platform.io.SearchResultsData;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class ModpackCachePlatformApi implements IPlatformApi {

    private IPlatformApi innerApi;
    private Cache<String, PlatformPackInfo> cache;
    private Cache<String, Boolean> deadPacks;
    private Cache<String, PlatformPackInfo> foreverCache;
    private LauncherDirectories directories;

    public ModpackCachePlatformApi(IPlatformApi innerApi, int cacheInSeconds, LauncherDirectories directories) {
        this.innerApi = innerApi;
        this.directories = directories;
        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds, TimeUnit.SECONDS)
                .build();

        foreverCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .build();

        deadPacks = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds / 10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public PlatformPackInfo getPlatformPackInfoForBulk(String packSlug) throws RestfulAPIException {

        PlatformPackInfo info = foreverCache.getIfPresent(packSlug);

        if (info == null) {
            info = loadForeverCache(packSlug);
        }

        if (info == null && isDead(packSlug))
            return null;

        if (info == null) {
            info = pullAndCache(packSlug);
        }

        return info;
    }

    @Override
    public PlatformPackInfo getPlatformPackInfo(String packSlug) throws RestfulAPIException {
        PlatformPackInfo info = cache.getIfPresent(packSlug);

        if (info == null && isDead(packSlug))
            return getDeadPackInfo(packSlug);

        try {
            if (info == null) {
                info = pullAndCache(packSlug);
            }
        } catch (RestfulAPIException ex) {
            ex.printStackTrace();

            deadPacks.put(packSlug, true);
            return getDeadPackInfo(packSlug);
        }

        return info;
    }

    protected PlatformPackInfo getDeadPackInfo(String packSlug) {
        try {
            PlatformPackInfo deadInfo = getPlatformPackInfoForBulk(packSlug);

            if (deadInfo != null)
                deadInfo.setLocal();
            return deadInfo;
        } catch (RestfulAPIException ex) {
            return null;
        }
    }

    private boolean isDead(String packSlug) {
        Boolean isDead = deadPacks.getIfPresent(packSlug);

        if (isDead != null && isDead.booleanValue())
            return true;

        return false;
    }

    private PlatformPackInfo pullAndCache(String packSlug) throws RestfulAPIException {
        PlatformPackInfo info = null;
        try {
            info = innerApi.getPlatformPackInfoForBulk(packSlug);

            if (info != null) {
                cache.put(packSlug, info);
                foreverCache.put(packSlug, info);
                saveForeverCache(info);
            }
        } finally {
            deadPacks.put(packSlug, info == null);
        }

        return info;
    }

    private PlatformPackInfo loadForeverCache(String packSlug) {
        File cacheFile = new File(new File(new File(directories.getAssetsDirectory(), "packs"), packSlug), "cache.json");
        if (!cacheFile.exists())
            return null;

        try {
            String packCache = FileUtils.readFileToString(cacheFile, Charset.forName("UTF-8"));
            PlatformPackInfo info = Utils.getGson().fromJson(packCache, PlatformPackInfo.class);

            if (info != null) {
                foreverCache.put(packSlug, info);
            }

            return info;
        } catch (IOException ex) {
            return null;
        } catch (JsonSyntaxException ex) {
            return null;
        }
    }

    private void saveForeverCache(PlatformPackInfo info) {
        File cacheFile = new File(new File(new File(directories.getAssetsDirectory(), "packs"), info.getName()), "cache.json");

        String packCache = Utils.getGson().toJson(info);

        try {
            FileUtils.writeStringToFile(cacheFile, packCache, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return;
        }
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
}
