package net.technicpack.discord;

import net.technicpack.discord.io.Server;
import net.technicpack.launchercore.modpacks.ModpackModel;

public interface IDiscordCallback {
    void discordCallback(ModpackModel pack, Server server);
}
