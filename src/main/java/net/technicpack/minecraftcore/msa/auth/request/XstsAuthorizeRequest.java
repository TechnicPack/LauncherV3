package net.technicpack.minecraftcore.msa.auth.request;

public class XstsAuthorizeRequest {
    private String RelyingParty;
    private String TokenType = "JWT";
    private XstsAuthorizeProperties Properties;

    public XstsAuthorizeRequest(String relyingParty, String deviceToken, String titleToken, String userToken) {
        RelyingParty = relyingParty;
        Properties = new XstsAuthorizeProperties(deviceToken, titleToken, userToken);
    }
}
