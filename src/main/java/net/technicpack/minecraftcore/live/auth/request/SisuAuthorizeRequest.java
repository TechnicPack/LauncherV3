package net.technicpack.minecraftcore.live.auth.request;

import java.security.interfaces.ECPublicKey;

public class SisuAuthorizeRequest {
    private String AccessToken;
    private String AppId = "00000000402b5328";
    private String DeviceToken;
    private String Sandbox = "RETAIL";
    private String SiteName = "user.auth.xboxlive.com";
    private String SessionId;
    private LiveProofKey ProofKey = new LiveProofKey();

    public SisuAuthorizeRequest(String accessToken, String deviceToken, String sessionId) {
        AccessToken = accessToken;
        DeviceToken = deviceToken;
        SessionId = sessionId;
    }

    public void setPublicKey(ECPublicKey pk)
    {
        ProofKey.setPublicKey(pk);
    }

}
