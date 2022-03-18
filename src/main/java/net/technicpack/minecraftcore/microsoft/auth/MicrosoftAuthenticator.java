package net.technicpack.minecraftcore.microsoft.auth;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport.Builder;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.model.*;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import static net.technicpack.launchercore.exception.MicrosoftAuthException.ExceptionType.*;

public class MicrosoftAuthenticator {
    private static final HttpTransport HTTP_TRANSPORT = (new Builder()).build();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private final HttpRequestFactory REQUEST_FACTORY;

    // OAUTH
    private static final String TECHNIC_CLIENT_ID = "8dfabc1d-38a9-42d8-bc08-677dbc60fe65";
    private static final String[] SCOPES = { "XboxLive.signin", "XboxLive.offline_access" };
    private static final String TOKEN_SERVER_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String AUTHORIZATION_SERVER_URL =
            "https://login.live.com/oauth20_authorize.srf?prompt=select_account&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d";
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    // XBOX
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    // MINECRAFT
    private static final String MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/launcher/login";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public MicrosoftAuthenticator(File dataStore) {
//        Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.ALL);
//        Logger.getLogger(HttpTransport.class.getName()).addHandler(new Handler() {
//            @Override
//            public void publish(LogRecord record) {
//                Utils.getLogger().log(Level.INFO, record.getMessage());
//            }
//
//            @Override
//            public void flush() {
//            }
//
//            @Override
//            public void close() throws SecurityException {
//
//            }
//        });
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
        final String tempCredentialId = UUID.randomUUID().toString();

        Credential credential = getOAuthCredential(tempCredentialId);

        XboxResponse xboxResponse = authenticateXbox(credential);
        XboxResponse xstsResponse = authenticateXSTS(xboxResponse.token);
        XboxMinecraftResponse xboxMinecraftResponse = authenticateMinecraftXbox(xstsResponse);
        MinecraftProfile profile = getMinecraftProfile(xboxMinecraftResponse);

        // TODO: what happens if the user changes their Minecraft username?
        updateCredentialStore(profile.name, credential);

        return new MicrosoftUser(xboxMinecraftResponse, profile);
    }

    public void refreshSession(MicrosoftUser user) throws AuthenticationException {
        // Refresh the OAuth token (should last 90 inactive days, indefinitely otherwise).
        // If it fails then we bail.
        Credential credential = loadExistingCredential(user.getUsername());

        if (credential == null) {
            throw new SessionException("Microsoft login expired or is invalid");
        }

        try {
            if (!credential.refreshToken()) {
                // Refresh request failed
                throw new SessionException("Microsoft login expired or is invalid");
            }
        } catch (IOException e) {
            // A 4xx was received
            throw new SessionException("Microsoft login expired or is invalid", e);
        }

        // Store the updated credential in the filesystem
        updateCredentialStore(user.getUsername(), credential);

        // Request the Xbox token
        XboxResponse xboxResponse = authenticateXbox(credential);

        // Request the XSTS token
        XboxResponse xstsResponse = authenticateXSTS(xboxResponse.token);

        // Request the Mojang token and store it (in memory)
        XboxMinecraftResponse xboxMinecraftResponse = authenticateMinecraftXbox(xstsResponse);
        user.updateAuthToken(xboxMinecraftResponse);
    }

    private Credential getOAuthCredential(String username) throws MicrosoftAuthException {
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setHost("localhost").setPort(-1).setCallbackPath("/").build();

        try {
            AuthorizationCodeFlow flow = buildFlow();
            return new AuthorizationCodeInstalledApp(flow, receiver, DesktopUtils::browseUrl).authorize(username);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get OAuth authorization", e);
            throw new MicrosoftAuthException(OAUTH, "Failed to get OAuth authorization", e);
        }
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
        request.setThrowExceptionOnExecuteError(false);
        request.setLoggingEnabled(true);
        request.setCurlLoggingEnabled(true);

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            return httpResponse.parseAs(XboxResponse.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get Xbox authentication.", e);
            throw new MicrosoftAuthException(XBOX, "Failed to get Xbox authentication.", e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private XboxResponse authenticateXSTS(String token) throws MicrosoftAuthException {
        XSTSRequest xstsRequest = new XSTSRequest(token);

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xstsRequest);
        HttpRequest request = buildPostRequest(XBOX_XSTS_URL, httpContent);

        // Don't throw on 401, that way we can handle the 401s we expect.
        request.setThrowExceptionOnExecuteError(false);
        request.setLoggingEnabled(true);
        request.setCurlLoggingEnabled(true);

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            httpResponse.setLoggingEnabled(true);

            switch (httpResponse.getStatusCode()) {
                case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED:
                    XSTSUnauthorized unauthorized = httpResponse.parseAs(XSTSUnauthorized.class);
                    throw new MicrosoftAuthException(unauthorized.getExceptionType(), "Failed to get XSTS authentication.");
                case HttpStatusCodes.STATUS_CODE_OK:
                    return httpResponse.parseAs(XboxResponse.class);
                default:
                    throw new HttpResponseException(httpResponse);
            }
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get XSTS authentication.", e);
            throw new MicrosoftAuthException(XSTS, "Failed to get XSTS authentication.", e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private XboxMinecraftResponse authenticateMinecraftXbox(XboxResponse xstsResponse) throws MicrosoftAuthException {
        XboxMinecraftRequest xboxMinecraftRequest = new XboxMinecraftRequest(xstsResponse);

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxMinecraftRequest);
        HttpRequest request = buildPostRequest(MINECRAFT_AUTH_URL, httpContent);
        request.setLoggingEnabled(true);
        request.setCurlLoggingEnabled(true);

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            return httpResponse.parseAs(XboxMinecraftResponse.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to authenticate with Xbox for Minecraft.", e);
            throw new MicrosoftAuthException(XBOX_MINECRAFT, "Failed to authenticate with Xbox for Minecraft.", e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private MinecraftProfile getMinecraftProfile(XboxMinecraftResponse xboxMinecraftResponse) throws MicrosoftAuthException {
        HttpRequest request = buildGetRequest(MINECRAFT_PROFILE_URL);
        String authorization = xboxMinecraftResponse.getAuthorization();
        request.setHeaders(request.getHeaders().setAuthorization(authorization));

        // Profile API returns 404 when it returns a response for no purchased account
        request.setThrowExceptionOnExecuteError(false);
        request.setLoggingEnabled(true);
        request.setCurlLoggingEnabled(true);

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            httpResponse.setLoggingEnabled(true);

            switch (httpResponse.getStatusCode()) {
                case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
                    MinecraftError minecraftError = httpResponse.parseAs(MinecraftError.class);
                    throw new MicrosoftAuthException(NO_MINECRAFT, "Minecraft Account Error: " + minecraftError.error);
                case HttpStatusCodes.STATUS_CODE_OK:
                    return httpResponse.parseAs(MinecraftProfile.class);
                default:
                    throw new HttpResponseException(httpResponse);
            }
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to load minecraft profile.", e);
            throw new MicrosoftAuthException(MINECRAFT_PROFILE, "Failed to load minecraft profile.", e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException ignored) {
                }
            }
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
                        .setScopes(Arrays.asList(SCOPES))
                        .setDataStoreFactory(DATA_STORE_FACTORY)
//                            .enablePKCE() TODO: Figure out PKCE
                        .build();
    }

    private void updateCredentialStore(String username, Credential credential) {
        try {
            // Remove all the other credentials
            DATA_STORE_FACTORY.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID).clear();

            // Store the new one
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
