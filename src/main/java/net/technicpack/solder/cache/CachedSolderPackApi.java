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
import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class CachedSolderPackApi implements ISolderPackApi {

    private LauncherDirectories directories;
    private ISolderPackApi innerApi;
    private int cacheInSeconds;
    private String packSlug;

    private SolderPackInfo rootInfoCache = null;
    private DateTime lastInfoAccess = new DateTime(0);

    private Cache<String, Modpack> buildCache;
    private Cache<String, Boolean> deadBuildCache;

    public CachedSolderPackApi(LauncherDirectories directories, ISolderPackApi innerApi, int cacheInSeconds, String packSlug) {
        this.directories = directories;
        this.innerApi = innerApi;
        this.cacheInSeconds = cacheInSeconds;
        this.packSlug = packSlug;

        buildCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds, TimeUnit.SECONDS)
                .build();

        deadBuildCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheInSeconds / 10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getMirrorUrl() {
        return innerApi.getMirrorUrl();
    }

    @Override
    public SolderPackInfo getPackInfoForBulk() throws RestfulAPIException {
        if (rootInfoCache != null)
            return rootInfoCache;

        loadForeverCache();

        if (rootInfoCache != null)
            return rootInfoCache;

        return pullAndCache();
    }

    @Override
    public SolderPackInfo getPackInfo() throws RestfulAPIException {
        if (Seconds.secondsBetween(lastInfoAccess, DateTime.now()).isLessThan(Seconds.seconds(cacheInSeconds))) {
            if (rootInfoCache != null)
                return rootInfoCache;
        }

        if (Seconds.secondsBetween(lastInfoAccess, DateTime.now()).isLessThan(Seconds.seconds(cacheInSeconds / 10)))
            return rootInfoCache;

        try {
            return pullAndCache();
        } catch (RestfulAPIException ex) {
            ex.printStackTrace();

            return getPackInfoForBulk();
        }
    }

    private SolderPackInfo pullAndCache() throws RestfulAPIException {
        try {
            rootInfoCache = innerApi.getPackInfoForBulk();
            saveForeverCache(rootInfoCache);
            return rootInfoCache;
        } finally {
            lastInfoAccess = DateTime.now();
        }
    }

    private void loadForeverCache() {
        File cacheFile = new File(new File(new File(directories.getAssetsDirectory(), "packs"), packSlug), "soldercache.json");
        if (!cacheFile.exists())
            return;

        try {
            String packCache = FileUtils.readFileToString(cacheFile, Charset.forName("UTF-8"));
            rootInfoCache = Utils.getGson().fromJson(packCache, SolderPackInfo.class);

            if (rootInfoCache != null)
                rootInfoCache.setLocal();
        } catch (IOException ex) {
        } catch (JsonSyntaxException ex) {
        }
    }

    private void saveForeverCache(SolderPackInfo info) {
        File cacheFile = new File(new File(new File(directories.getAssetsDirectory(), "packs"), info.getName()), "soldercache.json");

        String packCache = Utils.getGson().toJson(info);

        try {
            FileUtils.writeStringToFile(cacheFile, packCache, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return;
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
