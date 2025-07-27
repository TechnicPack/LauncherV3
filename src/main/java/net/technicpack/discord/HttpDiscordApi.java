package net.technicpack.discord;

import net.technicpack.discord.io.Server;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

public class HttpDiscordApi implements IDiscordApi {

    private final String baseUrl;

    public HttpDiscordApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void retrieveServer(final ModpackModel modpack, final String serverId, final IDiscordCallback callback) {
        new SwingWorker<Server, Void>() {
            @Override
            public Server doInBackground() {
                String guildUrl = String.format("%s/guilds/%s/widget.json", baseUrl, serverId);

                try {
                    return RestObject.getRestObject(Server.class, guildUrl);
                } catch (RestfulAPIException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            public void done() {
                try {
                    Server server = get();

                    callback.discordCallback(modpack, server);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
