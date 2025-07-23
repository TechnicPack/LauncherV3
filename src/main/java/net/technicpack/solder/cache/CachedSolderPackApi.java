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
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CachedSolderPackApi implements ISolderPackApi {

    private final ISolderPackApi innerApi;
    private final int cacheInSeconds;
    private final String packSlug;

    private SolderPackInfo rootInfoCache = null;
    private DateTime lastInfoAccess = new DateTime(0);

    private final Cache<String, Modpack> buildCache;
    private final Cache<String, Boolean> deadBuildCache;

    private final Path cachePath;

    public CachedSolderPackApi(LauncherDirectories directories, ISolderPackApi innerApi, int cacheInSeconds, String packSlug) {
        this.innerApi = innerApi;
        this.cacheInSeconds = cacheInSeconds;
        this.packSlug = packSlug;
        this.cachePath = directories.getAssetsDirectory().toPath()
                                    .resolve("packs")
                                    .resolve(packSlug)
                                    .resolve("soldercache.json");

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
        if (Seconds.secondsBetween(lastInfoAccess, DateTime.now())
                   .isLessThan(Seconds.seconds(cacheInSeconds)) && rootInfoCache != null) {
            return rootInfoCache;
        }

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
            saveForeverCache();
            return rootInfoCache;
        } finally {
            lastInfoAccess = DateTime.now();
        }
    }

    private void loadForeverCache() {
        if (!Files.exists(cachePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8)){
            rootInfoCache = Utils.getGson().fromJson(reader, SolderPackInfo.class);

            if (rootInfoCache == null) {
                return;
            }

            rootInfoCache.setLocal();
            rootInfoCache.setSolder(innerApi);
        } catch (JsonParseException | IOException ex) {
            Utils.getLogger().log(Level.SEVERE, String.format("Failed to load Solder cache for modpack \"%s\"", packSlug), ex);
        }
    }

    private void saveForeverCache() {
        try (Writer writer = Files.newBufferedWriter(cachePath, StandardCharsets.UTF_8)) {
            Utils.getGson().toJson(rootInfoCache, writer);
        } catch (JsonIOException | IOException ex) {
            Utils.getLogger().log(Level.SEVERE, String.format("Failed to save Solder cache for modpack \"%s\"", packSlug), ex);
        }
    }


    @Override
    public Modpack getPackBuild(String build) throws BuildInaccessibleException {

        Boolean isDead = deadBuildCache.getIfPresent(build);

        if (Boolean.TRUE.equals(isDead))
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
