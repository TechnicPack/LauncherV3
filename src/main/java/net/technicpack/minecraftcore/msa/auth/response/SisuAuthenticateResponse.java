package net.technicpack.minecraftcore.msa.auth.response;

import java.util.Map;

public class SisuAuthenticateResponse {
    private String MsaOauthRedirect;
    private Map<String, String> MsaRequestParameters;

    public SisuAuthenticateResponse() {
    }

    public String getMsaOauthRedirect() { return MsaOauthRedirect; }
}
