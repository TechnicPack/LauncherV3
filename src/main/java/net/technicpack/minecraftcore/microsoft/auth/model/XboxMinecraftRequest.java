package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

/**
 *
 */
public class XboxMinecraftRequest {
    @Key(value="identityToken") private String identityToken;

    public XboxMinecraftRequest(XboxResponse xstsResponse) {
        identityToken = "XBL3.0 x=" + xstsResponse.getUserhash() + ";" + xstsResponse.token;
    }
}
