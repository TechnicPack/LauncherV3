package net.technicpack.minecraftcore.live.auth.response;

public class SisuAuthorizeResponseToken {
    private String IssueInstant;
    private String NotAfter;
    private String Token;
    private XboxDisplayClaims DisplayClaims;

    SisuAuthorizeResponseToken() {
    }

    public String getToken() { return Token; }
    public XboxDisplayClaims getDisplayClaims() { return DisplayClaims; }
}
