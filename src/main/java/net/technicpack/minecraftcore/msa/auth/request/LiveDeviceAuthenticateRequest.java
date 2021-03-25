package net.technicpack.minecraftcore.msa.auth.request;

import java.security.interfaces.ECPublicKey;

public class LiveDeviceAuthenticateRequest {
    private String RelyingParty = "http://auth.xboxlive.com";
    private String TokenType = "JWT";
    private LiveDeviceAuthenticateProperties Properties = new LiveDeviceAuthenticateProperties();

    public LiveDeviceAuthenticateRequest() {

    }

    public void setPublicKey(ECPublicKey pk)
    {
        Properties.setPublicKey(pk);
    }
}
