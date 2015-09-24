package net.technicpack.discord;

import net.technicpack.launchercore.modpacks.ModpackModel;

public interface IDiscordApi {
    void retrieveServer(ModpackModel modpack, String serverId, IDiscordCallback callback);
}
