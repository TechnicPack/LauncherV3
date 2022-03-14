package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;
import net.technicpack.launchercore.exception.MicrosoftAuthException;

/**
 *
 */
public class XboxMinecraftRequest {
    @Key(value="xtoken") private String identityToken;
    @Key(value="platform") final private String platform = "PC_LAUNCHER";

    public XboxMinecraftRequest(XboxResponse xstsResponse) throws MicrosoftAuthException {
        identityToken = "XBL3.0 x=" + xstsResponse.getUserhash() + ";" + xstsResponse.token;
    }
}
