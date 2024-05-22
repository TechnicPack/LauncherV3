package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class XboxAuthProperties {
    @Key(value="AuthMethod") private final String authMethod = "RPS";
    @Key(value="SiteName") private final String siteName = "user.auth.xboxlive.com";
    @Key(value="RpsTicket") private final String rpsTicket;

    public XboxAuthProperties(String accessToken) {
        this.rpsTicket = "d=" + accessToken;
    }
}
