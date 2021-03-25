package net.technicpack.minecraftcore.msa.auth.request;

public class XstsAuthorizeProperties {
    private String SandboxId = "RETAIL";
    private String DeviceToken;
    private String TitleToken;
    private String[] UserTokens;

    public XstsAuthorizeProperties(String deviceToken, String titleToken, String userToken) {
        DeviceToken = deviceToken;
        TitleToken = titleToken;
        UserTokens = new String[1];
        UserTokens[0] = userToken;
    }
}
