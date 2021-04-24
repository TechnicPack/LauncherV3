package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;
import net.technicpack.launchercore.exception.MicrosoftAuthException;

/**
 *
 */
public class XboxMinecraftRequest {
    @Key(value="identityToken") private String identityToken;

    public XboxMinecraftRequest(XboxResponse xstsResponse) throws MicrosoftAuthException {
        identityToken = "XBL3.0 x=" + xstsResponse.getUserhash() + ";" + xstsResponse.token;
    }
}
