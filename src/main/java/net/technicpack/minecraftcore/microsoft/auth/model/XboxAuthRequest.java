package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class XboxAuthRequest {
    @Key(value="Properties") private final XboxAuthProperties properties;
    @Key(value="RelyingParty") private final String relyingParty = "http://auth.xboxlive.com";
    @Key(value="TokenType") private final String tokenType = "JWT";

    public XboxAuthRequest(String accessToken) {
        this.properties = new XboxAuthProperties(accessToken);
    }
}
