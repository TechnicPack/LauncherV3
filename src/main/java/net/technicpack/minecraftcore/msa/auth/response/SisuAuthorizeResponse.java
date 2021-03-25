package net.technicpack.minecraftcore.msa.auth.response;

public class SisuAuthorizeResponse {
    private String DeviceToken;
    private SisuAuthorizeResponseToken TitleToken;
    private SisuAuthorizeResponseToken UserToken;
    private SisuAuthorizeResponseToken AuthorizationToken;
    private String WebPage;
    private String Sandbox;

    public SisuAuthorizeResponse() {
    }

    public String GetTitleToken() { return TitleToken.getToken(); }
    public XboxDisplayClaims GetTitleDisplayClaims() { return TitleToken.getDisplayClaims(); }
    public String GetUserToken() { return UserToken.getToken(); }
    public XboxDisplayClaims GetUserDisplayClaims() { return UserToken.getDisplayClaims(); }
    public String GetAuthorizationToken() { return AuthorizationToken.getToken(); }
    public XboxDisplayClaims GetAuthorizationDisplayClaims() { return AuthorizationToken.getDisplayClaims(); }
}
