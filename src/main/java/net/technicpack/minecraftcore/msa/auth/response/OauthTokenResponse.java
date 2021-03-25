package net.technicpack.minecraftcore.msa.auth.response;

public class OauthTokenResponse {
    private String token_type;
    private int expires_in;
    private String scope;
    private String access_token;
    private String refresh_token;
    private String user_id;
    // "foci": "1"

    public OauthTokenResponse() {
    }

    public String GetAccessToken() { return access_token; }
    public String GetRefreshToken() { return refresh_token; }
}
