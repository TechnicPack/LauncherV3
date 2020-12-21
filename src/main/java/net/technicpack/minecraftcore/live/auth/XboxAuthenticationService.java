package net.technicpack.minecraftcore.live.auth;

import com.google.common.base.Charsets;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.live.Base64;
import net.technicpack.minecraftcore.live.auth.request.*;
import net.technicpack.minecraftcore.live.auth.response.*;
import net.technicpack.minecraftcore.mojang.auth.response.AuthResponse;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class XboxAuthenticationService {
    private XboxSignature xboxSignature = new XboxSignature();
    private String deviceToken = null;
    private String sessionId = null;
    private String codeVerifier = null;
    private String LiveOauthAccessToken = null;
    private String LiveOauthRefreshToken = null;
    private String SisuTitleToken = null;
    private String SisuUserToken = null;
    private String SisuAuthorizationToken = null;
    private String MinecraftOauthAccessToken = null;
    private String userHash = null;
    private String xstsToken = null;
    private String ServicesHost = null;
    private String ServicesRP = null;

    public XboxAuthenticationService() {
        try {
            byte[] bytes = new byte[0x40];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            codeVerifier = Base64.encodeToString(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String postJson(String url, Map<String, String> headers, String data) throws IOException {
        byte[] rawData = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setRequestProperty("Content-Length", Integer.toString(rawData.length));
        for (Map.Entry<String,String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(rawData);
        writer.flush();
        writer.close();

        InputStream stream = null;
        String returnable = null;
        try {
            stream = connection.getInputStream();
            returnable = IOUtils.toString(stream, Charsets.UTF_8);
            String sid = connection.getHeaderField("X-SessionId");
            if (sid != null)
                sessionId = sid;
        } catch (IOException e) {
            stream = connection.getErrorStream();

            if (stream == null) {
                throw e;
            }
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {}
        }

        return returnable;
    }

    private String postForm(String url, String data) throws IOException {
        byte[] rawData = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setRequestProperty("Content-Length", Integer.toString(rawData.length));

        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.write(rawData);
        writer.flush();
        writer.close();

        InputStream stream = null;
        String returnable = null;
        try {
            stream = connection.getInputStream();
            returnable = IOUtils.toString(stream, Charsets.UTF_8);
            String sid = connection.getHeaderField("X-SessionId");
            if (sid != null)
                sessionId = sid;
        } catch (IOException e) {
            stream = connection.getErrorStream();

            if (stream == null) {
                throw e;
            }
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {}
        }

        return returnable;
    }

    private String getJson(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        for (Map.Entry<String,String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        InputStream stream = null;
        String returnable = null;
        try {
            stream = connection.getInputStream();
            returnable = IOUtils.toString(stream, Charsets.UTF_8);
            String sid = connection.getHeaderField("X-SessionId");
            if (sid != null)
                sessionId = sid;
        } catch (IOException e) {
            stream = connection.getErrorStream();

            if (stream == null) {
                throw e;
            }
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {}
        }

        return returnable;
    }

    public String GetDeviceToken() {
        if (deviceToken != null)
            return deviceToken;

        try {
            LiveDeviceAuthenticateRequest lda = new LiveDeviceAuthenticateRequest();
            lda.setPublicKey(xboxSignature.getPublicKey());

            String data = MojangUtils.getUglyGson().toJson(lda);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-xbl-contract-version", "1");
            String signature = xboxSignature.SignRequest("POST", "https://device.auth.xboxlive.com/device/authenticate", headers, data);
            headers.put("Signature", signature);

            String returned = postJson("https://device.auth.xboxlive.com/device/authenticate", headers, data);

            LiveDeviceAuthenticateResponse response = MojangUtils.getGson().fromJson(returned, LiveDeviceAuthenticateResponse.class);
            deviceToken = response.getToken();
            return deviceToken;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String GetOauthUrl() {
        try {
            Map<String, String> query = new HashMap<String, String>();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());
            String codeChallenge = Base64.encodeToString(hash, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

            query.put("code_challenge", codeChallenge);
            query.put("code_challenge_method", "S256");

            SisuAuthenticateRequest sa = new SisuAuthenticateRequest(GetDeviceToken(), query);

            String data = MojangUtils.getUglyGson().toJson(sa);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-xbl-contract-version", "1");
            String signature = xboxSignature.SignRequest("POST", "https://sisu.xboxlive.com/authenticate", headers, data);
            headers.put("Signature", signature);

            String returned = postJson("https://sisu.xboxlive.com/authenticate", headers, data);

            SisuAuthenticateResponse response = MojangUtils.getGson().fromJson(returned, SisuAuthenticateResponse.class);
            return response.getMsaOauthRedirect();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void LiveAuthToken(String code) {
        try {
            Map<String, String> query = new HashMap<String, String>();
            query.put("client_id", "00000000402b5328");
            query.put("code", code);
            query.put("code_verifier", codeVerifier);
            query.put("grant_type", "authorization_code");
            query.put("redirect_uri", "https://login.live.com/oauth20_desktop.srf");
            query.put("scope", "service::user.auth.xboxlive.com::MBI_SSL");

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String,String> entry : query.entrySet())
            {
                sb.append(entry.getKey());
                if (entry.getValue() != null) {
                    sb.append('=');
                    sb.append(entry.getValue());
                }
                sb.append('&');
            }
            sb.deleteCharAt(sb.length() - 1);

            String returned = postForm("https://login.live.com/oauth20_token.srf", sb.toString());

            OauthTokenResponse response = MojangUtils.getGson().fromJson(returned, OauthTokenResponse.class);

            LiveOauthAccessToken = response.GetAccessToken();
            LiveOauthRefreshToken = response.GetRefreshToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LiveRefreshToken(String refreshToken) {
        LiveOauthRefreshToken = refreshToken;
        try {
            Map<String, String> query = new HashMap<String, String>();
            query.put("client_id", "00000000402b5328");
            query.put("refresh_token", refreshToken);
            query.put("grant_type", "refresh_token");
            query.put("scope", "service::user.auth.xboxlive.com::MBI_SSL");

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String,String> entry : query.entrySet())
            {
                sb.append(entry.getKey());
                if (entry.getValue() != null) {
                    sb.append('=');
                    sb.append(entry.getValue());
                }
                sb.append('&');
            }
            sb.deleteCharAt(sb.length() - 1);

            String returned = postForm("https://login.live.com/oauth20_token.srf", sb.toString());

            OauthTokenResponse response = MojangUtils.getGson().fromJson(returned, OauthTokenResponse.class);

            LiveOauthAccessToken = response.GetAccessToken();
            refreshToken = response.GetRefreshToken();
            if (refreshToken != null)
                LiveOauthRefreshToken = refreshToken;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void GetSisuTokens() {
        try {
            SisuAuthorizeRequest sa = new SisuAuthorizeRequest("t=" + LiveOauthAccessToken, GetDeviceToken(), sessionId);
            sa.setPublicKey(xboxSignature.getPublicKey());

            String data = MojangUtils.getUglyGson().toJson(sa);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-xbl-contract-version", "1");
            String signature = xboxSignature.SignRequest("POST", "https://sisu.xboxlive.com/authorize", headers, data);
            headers.put("Signature", signature);

            String returned = postJson("https://sisu.xboxlive.com/authorize", headers, data);

            SisuAuthorizeResponse response = MojangUtils.getGson().fromJson(returned, SisuAuthorizeResponse.class);

            SisuTitleToken = response.GetTitleToken();
            SisuUserToken = response.GetUserToken();
            SisuAuthorizationToken = response.GetAuthorizationToken();
            userHash = response.GetAuthorizationDisplayClaims().GetXui()[0].get("uhs");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String GetSisuTitleToken() {
        if (SisuTitleToken == null)
            GetSisuTokens();
        return SisuTitleToken;
    }

    public String GetSisuUserToken() {
        if (SisuUserToken == null)
            GetSisuTokens();
        return SisuUserToken;
    }

    public String GetUserHash() {
        if (userHash == null)
            GetSisuTokens();
        return userHash;
    }

    public String GetSisuAuthorizationToken() {
        if (SisuAuthorizationToken == null)
            GetSisuTokens();
        return SisuAuthorizationToken;
    }

    private void GetEndpoints()
    {
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "XBL3.0 x=" + GetUserHash() + ";" + GetSisuAuthorizationToken());
            headers.put("x-xbl-contract-version", "1");
            String signature = xboxSignature.SignRequest("GET", "https://title.mgt.xboxlive.com/titles/current/endpoints", headers, "");
            headers.put("Signature", signature);

            String returned = getJson("https://title.mgt.xboxlive.com/titles/current/endpoints", headers);

            TitleEndpoitsResponse response = MojangUtils.getGson().fromJson(returned, TitleEndpoitsResponse.class);

            for (Map<String, String> EndPoint : response.GetEndPoints())
            {
                if (EndPoint.get("TokenType").equals("JWT"))
                {
                    ServicesHost = EndPoint.get("Host");
                    ServicesRP = EndPoint.get("RelyingParty");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String GetServicesHost()
    {
        if (ServicesHost == null)
            GetEndpoints();
        return ServicesHost;
    }

    private String GetServicesRP()
    {
        if (ServicesRP == null)
            GetEndpoints();
        return ServicesRP;
    }

    private void GetXstsToken() {
        try {
            XstsAuthorizeRequest xa = new XstsAuthorizeRequest(GetServicesRP(), GetDeviceToken(), GetSisuTitleToken(), GetSisuUserToken());

            String data = MojangUtils.getUglyGson().toJson(xa);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-xbl-contract-version", "1");
            String signature = xboxSignature.SignRequest("POST", "https://xsts.auth.xboxlive.com/xsts/authorize", headers, data);
            headers.put("Signature", signature);

            String returned = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", headers, data);

            XstsAuthorizeResponse response = MojangUtils.getGson().fromJson(returned, XstsAuthorizeResponse.class);

            userHash = response.GetDisplayClaims().GetXui()[0].get("uhs");
            xstsToken = response.getToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String GetMinecraftAuthorization() {
        if (MinecraftOauthAccessToken != null)
            return "Bearer " + MinecraftOauthAccessToken;

        try {
            if (userHash == null || xstsToken == null)
                GetXstsToken();

            LoginWithXboxRequest sa = new LoginWithXboxRequest("XBL3.0 x=" + userHash + ";" + xstsToken);

            String data = MojangUtils.getUglyGson().toJson(sa);

            Map<String, String> headers = new HashMap<String, String>();

            String returned = postJson("https://" + GetServicesHost() + "/authentication/login_with_xbox", headers, data);

            OauthTokenResponse response = MojangUtils.getGson().fromJson(returned, OauthTokenResponse.class);

            MinecraftOauthAccessToken = response.GetAccessToken();
            return "Bearer " + MinecraftOauthAccessToken;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public MinecraftProfile GetMinecraftProfile()
    {
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", GetMinecraftAuthorization());

            String returned = getJson("https://" + GetServicesHost() + "/minecraft/profile", headers);

            MinecraftProfile response = MojangUtils.getGson().fromJson(returned, MinecraftProfile.class);

            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String GetLiveRefreshToken() { return LiveOauthRefreshToken; }
    public String GetMinecraftAccessToken() { return MinecraftOauthAccessToken; }
}
