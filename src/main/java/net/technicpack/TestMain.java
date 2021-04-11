package net.technicpack;

import com.google.api.client.auth.oauth2.Credential;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxResponse;

public class TestMain {
    public static void main(String[] argv) throws Exception {
        MicrosoftAuthenticator microsoftAuthenticator = new MicrosoftAuthenticator();

        Credential credential = microsoftAuthenticator.getOAuthCredential("arili");

        microsoftAuthenticator.authenticateOAuth(credential);

        XboxResponse xboxResponse = microsoftAuthenticator.authenticateXbox(credential);
//        System.out.println(xboxResponse.token);
//        System.out.println(xboxResponse.getUserhash());

        XboxResponse xstsResponse = microsoftAuthenticator.authenticateXSTS(xboxResponse);
//        System.out.println(xstsResponse.token);
//        System.out.println(xboxResponse.getUserhash());

        XboxMinecraftResponse xboxMinecraftResponse = microsoftAuthenticator.authenticateMinecraftXbox(xstsResponse);
//        System.out.println(xboxMinecraftResponse);

        MinecraftProfile profile = microsoftAuthenticator.getMinecraftProfile(xboxMinecraftResponse);
        System.out.println(profile);

        microsoftAuthenticator.updateCredentialStore(profile.name, credential);
        System.out.println("end");
    }


}
