package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;
import net.technicpack.launchercore.exception.MicrosoftAuthException;

public class XboxResponse {
    @Key(value="IssueInstant") public String issueInstant;
    @Key(value="NotAfter") public String notAfter;
    @Key(value="Token") public String token;
    @Key(value="DisplayClaims") public DisplayClaims displayClaims;

    public String getUserhash() throws MicrosoftAuthException {
        return displayClaims.getUserhash();
    }

}
