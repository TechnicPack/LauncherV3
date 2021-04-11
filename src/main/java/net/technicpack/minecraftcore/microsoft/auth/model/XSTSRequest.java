package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class XSTSRequest {
    @Key(value="Properties") private XSTSProperties properties;
    @Key(value="RelyingParty") private String relyingParty = "rp://api.minecraftservices.com/";
    @Key(value="TokenType") private String tokenType = "JWT";

    public XSTSRequest(String token) {
        properties = new XSTSProperties(token);
    }
}
