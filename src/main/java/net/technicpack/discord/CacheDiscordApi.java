package net.technicpack.discord;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.technicpack.discord.io.Server;
import net.technicpack.launchercore.modpacks.ModpackModel;

import java.util.concurrent.TimeUnit;

public class CacheDiscordApi implements IDiscordApi {

    private IDiscordApi innerApi;
    private Cache<String, Server> cache;
    private Cache<String, Boolean> deadCache;

    public CacheDiscordApi(IDiscordApi innerApi, int cacheLength, int deadCacheLength) {
        this.innerApi = innerApi;
        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(cacheLength, TimeUnit.SECONDS)
                .build();

        deadCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(300)
                .expireAfterWrite(deadCacheLength, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void retrieveServer(ModpackModel modpack, final String serverId, final IDiscordCallback callback) {
        final Boolean deadCacheValue = deadCache.getIfPresent(serverId);

        if (deadCacheValue != null && deadCacheValue)
            return;

        Server cacheValue = cache.getIfPresent(serverId);
        if (cacheValue != null) {
            if (deadCacheValue == null)
                deadCache.put(serverId, false);

            callback.serverGetCallback(modpack, cacheValue);
            return;
        }

        //Fill the dead cache immediately to prevent discord API requests from getting spammed
        //before the first one comes back.  The response will sort out the "correct" value for the
        //dead cache.
        deadCache.put(serverId, true);
        this.innerApi.retrieveServer(modpack, serverId, new IDiscordCallback() {
            @Override
            public void serverGetCallback(ModpackModel pack, Server server) {
                if (server == null)
                    deadCache.put(serverId, true);
                else {
                    deadCache.put(serverId, false);
                    cache.put(serverId, server);
                }

                callback.serverGetCallback(pack, server);
            }
        });
    }
}
