package net.technicpack.minecraftcore.microsoft.auth;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.model.*;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;

import static net.technicpack.launchercore.exception.MicrosoftAuthException.ExceptionType.*;

public class MicrosoftAuthenticator {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private final HttpRequestFactory REQUEST_FACTORY;

    // OAUTH
    private static final String TECHNIC_CLIENT_ID = "5f8b309f-ad5f-49bf-877a-8b94afd75b9f";
    private static final String SCOPE = "XboxLive.signin";
    private static final String TOKEN_SERVER_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String AUTHORIZATION_SERVER_URL =
            "https://login.live.com/oauth20_authorize.srf?prompt=select_account&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d";
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    // XBOX
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    // MINECRAFT
    private static final String MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public MicrosoftAuthenticator(File dataStore) {
        REQUEST_FACTORY = HTTP_TRANSPORT.createRequestFactory(
                request -> request.setParser(new JsonObjectParser(JSON_FACTORY))
        );

        try {
            DATA_STORE_FACTORY = new FileDataStoreFactory(dataStore);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to setup credential store.", e);
        }
    }

    public MicrosoftUser loginNewUser() throws MicrosoftAuthException {
        Credential credential = getOAuthCredential(UUID.randomUUID().toString());

        XboxResponse xboxResponse = authenticateXbox(credential);
        XboxResponse xstsResponse = authenticateXSTS(xboxResponse.token);
        XboxMinecraftResponse xboxMinecraftResponse = authenticateMinecraftXbox(xstsResponse);
        MinecraftProfile profile = getMinecraftProfile(xboxMinecraftResponse);

        updateCredentialStore(profile.name, credential);

        return new MicrosoftUser(xboxResponse, xboxMinecraftResponse, profile);
    }

    public void refreshSession(MicrosoftUser user) throws AuthenticationException {
        // We have an xbox token still, so we should be able to just refresh.
        if (user.getXboxExpiresInSeconds() > 60) {
            XboxResponse xstsResponse = authenticateXSTS(user.getXboxAccessToken());
            XboxMinecraftResponse authResponse = authenticateMinecraftXbox(xstsResponse);
            user.updateAuthToken(authResponse);
            return;
        }

        Credential credential = loadExistingCredential(user.getUsername());

        // Somehow our xbox token is expired but our OAuth token is still good.
        if (isOauthCredentialActive(credential)) {
            XboxResponse xboxResponse = authenticateXbox(credential);
            user.updateXboxToken(xboxResponse);
            XboxMinecraftResponse authResponse = authenticateMinecraftXbox(authenticateXSTS(xboxResponse.token));
            user.updateAuthToken(authResponse);
            return;
        }

        // Session is expired now, no saving it
        throw new SessionException("Microsoft login expired or missing.");
    }

    private Credential getOAuthCredential(String username) throws MicrosoftAuthException {
        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setHost("localhost").setPort(0).setCallbackPath("/").build();

        try {
            AuthorizationCodeFlow flow = buildFlow();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(username);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get OAuth authorization", e);
            throw new MicrosoftAuthException(OAUTH, "Failed to get OAuth authorization", e);
        }
    }

    private boolean isOauthCredentialActive(Credential credential) {
        return credential != null
                && (credential.getRefreshToken() != null ||
                credential.getExpiresInSeconds() != null ||
                credential.getExpiresInSeconds() > 60);
    }

    private Credential loadExistingCredential(String username) throws MicrosoftAuthException {
        AuthorizationCodeFlow flow;

        try {
            flow = buildFlow();
            return flow.loadCredential(username);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to load from credential store.", e);
            throw new MicrosoftAuthException(OAUTH, "Failed to load from credential store.");
        }
    }

    private XboxResponse authenticateXbox(Credential credential) throws MicrosoftAuthException {
        XboxAuthRequest xboxAuthRequest = new XboxAuthRequest(credential.getAccessToken());

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxAuthRequest);
        HttpRequest request = buildPostRequest(XBOX_AUTH_URL, httpContent);
        request.setInterceptor(credential);
        request.setUnsuccessfulResponseHandler(credential);

        try {
            return request.execute().parseAs(XboxResponse.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get Xbox authentication.", e);
            throw new MicrosoftAuthException(XBOX, "Failed to get Xbox Authentication.", e);
        }
    }

    private XboxResponse authenticateXSTS(String token) throws MicrosoftAuthException {
        XSTSRequest xstsRequest = new XSTSRequest(token);

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xstsRequest);
        HttpRequest request = buildPostRequest(XBOX_XSTS_URL, httpContent);

        try {
            HttpResponse httpResponse = request.execute();
            if (httpResponse.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED) {
                XSTSUnauthorized unauthorized = httpResponse.parseAs(XSTSUnauthorized.class);
                throw new MicrosoftAuthException(unauthorized.getExceptionType(), "Failed to get XSTS authentication.");
            }
            return httpResponse.parseAs(XboxResponse.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get XSTS authentication.", e);
            throw new MicrosoftAuthException(XSTS, "Failed to get XSTS authentication.", e);
        }
    }

    private XboxMinecraftResponse authenticateMinecraftXbox(XboxResponse xstsResponse) throws MicrosoftAuthException {
        XboxMinecraftRequest xboxMinecraftRequest = new XboxMinecraftRequest(xstsResponse);

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxMinecraftRequest);
        HttpRequest request = buildPostRequest(MINECRAFT_AUTH_URL, httpContent);

        try {
            return request.execute().parseAs(XboxMinecraftResponse.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to authenticate with Xbox for Minecraft.", e);
            throw new MicrosoftAuthException(XBOX_MINECRAFT, "Failed to authenticate with Xbox for Minecraft.", e);
        }
    }

    private MinecraftProfile getMinecraftProfile(XboxMinecraftResponse xboxMinecraftResponse) throws MicrosoftAuthException {

        HttpRequest request = buildGetRequest(MINECRAFT_PROFILE_URL);
        // Remove the OAuth interceptor while doing this step.
        request.setInterceptor(null);
        String authorization = xboxMinecraftResponse.getAuthorization();
        request.setHeaders(request.getHeaders().setAuthorization(authorization));

        try {
            HttpResponse httpResponse = request.execute();
            MinecraftProfile minecraftProfile = httpResponse.parseAs(MinecraftProfile.class);
            if (minecraftProfile == null) {
                MinecraftError minecraftError = httpResponse.parseAs(MinecraftError.class);
                throw new MicrosoftAuthException(NO_MINECRAFT, "Minecraft Account Error: " + minecraftError.error);
            }
            return request.execute().parseAs(MinecraftProfile.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to load minecraft profile.", e);
            throw new MicrosoftAuthException(MINECRAFT_PROFILE, "Failed to load minecraft profile.", e);
        }
    }

    private AuthorizationCodeFlow buildFlow() throws IOException {
        return new AuthorizationCodeFlow.Builder(
                        BearerToken.authorizationHeaderAccessMethod(),
                        HTTP_TRANSPORT,
                        JSON_FACTORY,
                        new GenericUrl(TOKEN_SERVER_URL),
                        new ClientParametersAuthentication(TECHNIC_CLIENT_ID, null),
                        TECHNIC_CLIENT_ID,
                        AUTHORIZATION_SERVER_URL)
                        .setScopes(Collections.singletonList(SCOPE))
                        .setDataStoreFactory(DATA_STORE_FACTORY)
//                            .enablePKCE() TODO: Figure out PKCE
                        .build();
    }

    private void updateCredentialStore(String username, Credential credential) {
        try {
            DATA_STORE_FACTORY.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID)
                    .set(username, new StoredCredential(credential));
        } catch (IOException e) {
            e.printStackTrace();
            Utils.getLogger().log(Level.WARNING, "Failed to save user credential to the data store.");
        }
    }

    private HttpRequest buildGetRequest(String url) throws MicrosoftAuthException {
        try {
            return REQUEST_FACTORY.buildGetRequest(new GenericUrl(url));
        } catch (IOException e) {
            throw new MicrosoftAuthException(REQUEST, "Failed to build get request.", e);
        }
    }

    private HttpRequest buildPostRequest(String url, HttpContent httpContent) throws MicrosoftAuthException {
        try {
            return REQUEST_FACTORY.buildPostRequest(new GenericUrl(url), httpContent);
        } catch (IOException e) {
            throw new MicrosoftAuthException(REQUEST, "Failed to build post request.", e);
        }
    }
}
