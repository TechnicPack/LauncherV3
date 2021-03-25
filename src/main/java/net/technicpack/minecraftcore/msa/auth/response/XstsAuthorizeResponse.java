package net.technicpack.minecraftcore.msa.auth.response;

public class XstsAuthorizeResponse {
    private String IssueInstant;
    private String NotAfter;
    private String Token;
    private XboxDisplayClaims DisplayClaims;

    public XstsAuthorizeResponse() {
    }

    public String getToken() { return Token; }
    public XboxDisplayClaims GetDisplayClaims() { return DisplayClaims; }
}
