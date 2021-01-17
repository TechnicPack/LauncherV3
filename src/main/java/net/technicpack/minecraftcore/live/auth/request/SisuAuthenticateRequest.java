package net.technicpack.minecraftcore.live.auth.request;

import java.util.Map;

public class SisuAuthenticateRequest {
    private String AppId = "00000000402b5328";
    private String RedirectUri = "https://login.live.com/oauth20_desktop.srf";
    private String DeviceToken;
    private String Sandbox = "RETAIL";
    private String TokenType = "code";
    private String[] Offers = { "service::user.auth.xboxlive.com::MBI_SSL" };
    private Map<String, String> Query;

    public SisuAuthenticateRequest(String deviceToken, Map<String, String> query) {
        DeviceToken = deviceToken;
        Query = query;
    }
}
