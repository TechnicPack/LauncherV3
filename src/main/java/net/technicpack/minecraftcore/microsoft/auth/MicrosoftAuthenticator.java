package net.technicpack.minecraftcore.microsoft.auth;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.*;
import com.google.api.client.http.apache.v5.Apache5HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import io.sentry.Sentry;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.model.*;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import static net.technicpack.launchercore.exception.MicrosoftAuthException.ExceptionType.*;

public class MicrosoftAuthenticator {
    private static final HttpTransport HTTP_TRANSPORT = new Apache5HttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private final HttpRequestFactory REQUEST_FACTORY;

    // OAUTH
    private static final String TECHNIC_CLIENT_ID = "8dfabc1d-38a9-42d8-bc08-677dbc60fe65";
    private static final String[] SCOPES = { "XboxLive.signin", "XboxLive.offline_access" };
    private static final String TOKEN_SERVER_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String AUTHORIZATION_SERVER_URL =
            "https://login.live.com/oauth20_authorize.srf?prompt=select_account&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d";
    private final FileDataStoreFactory dataStoreFactory;

    // XBOX
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    // MINECRAFT
    private static final String MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/launcher/login";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String MINECRAFT_ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/license";

    private LocalServerReceiver receiver;

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
                request -> {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));

                    request.setLoggingEnabled(true);
                    request.setCurlLoggingEnabled(true);
                }
        );

        // Work around the Access Denied exception that sometimes happens due to Windows ACL permission issues
        Path dataStorePath = Paths.get(dataStore.getAbsolutePath());
        if (Files.exists(dataStorePath)) {
            try {
                // FileDataStoreFactory internally does this when it tries to lock down the permissions of the file.
                FileOwnerAttributeView fileAttributeView =
                        Files.getFileAttributeView(dataStorePath, FileOwnerAttributeView.class);
                fileAttributeView.getOwner();
            } catch (AccessDeniedException e) {
                // Attempt to delete the file if we get an AccessDeniedException
                try {
                    Files.delete(dataStorePath);
                } catch (IOException ex) {
                    Sentry.captureException(ex);
                    throw new RuntimeException(ex);
                }
            } catch (IOException ex) {
                Sentry.captureException(ex);
                // Ignore any other IO exceptions
            }
        }

        try {
            dataStoreFactory = new FileDataStoreFactory(dataStore);
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

        if (!checkMinecraftOwnership(getEntitlements(xboxMinecraftResponse))) {
            throw new MicrosoftAuthException(NO_MINECRAFT, "Microsoft account does not own Minecraft");
        }

        MinecraftProfile profile = getMinecraftProfile(xboxMinecraftResponse);

        // TODO: what happens if the user changes their Minecraft username?
        updateCredentialStore(profile.id, credential);

        return new MicrosoftUser(xboxMinecraftResponse, profile);
    }

    public void refreshSession(MicrosoftUser user) throws AuthenticationException {
        // Refresh the OAuth token (should last 90 inactive days, indefinitely otherwise).
        // If it fails then we bail.
        Credential credential = loadExistingCredential(user.getId());

        // TODO: migration path, remove in future release
        // Load from username as a fallback
        if (credential == null) {
            credential = loadExistingCredential(user.getUsername());
        }

        if (credential == null) {
            throw new SessionException("Microsoft login expired or is invalid");
        }

        try {
            if (!credential.refreshToken()) {
                // Refresh request failed
                throw new SessionException("Microsoft login expired or is invalid");
            }
        } catch (TokenResponseException e) {
            // A 4xx was received
            throw new SessionException("Microsoft login expired or is invalid", e);
        } catch (IOException e) {
            throw new AuthenticationException("Failed to refresh session", e);
        }

        // Store the updated credential in the filesystem
        updateCredentialStore(user.getId(), credential);

        // Request the Xbox token
        XboxResponse xboxResponse = authenticateXbox(credential);

        // Request the XSTS token
        XboxResponse xstsResponse = authenticateXSTS(xboxResponse.token);

        // Request the Mojang token and store it (in memory)
        XboxMinecraftResponse xboxMinecraftResponse = authenticateMinecraftXbox(xstsResponse);

        user.setAuthToken(xboxMinecraftResponse);

        // Check entitlements
        if (!checkMinecraftOwnership(getEntitlements(xboxMinecraftResponse))) {
            throw new SessionException("Microsoft account does not own Minecraft");
        }

        // Update Minecraft profile
        MinecraftProfile profile = getMinecraftProfile(xboxMinecraftResponse);

        user.setProfile(profile);
    }

    private Credential getOAuthCredential(String id) throws MicrosoftAuthException {
        receiver = new LocalServerReceiver.Builder()
                .setHost("localhost").setPort(-1).setCallbackPath("/").build();

        try {
            AuthorizationCodeFlow flow = buildFlow();
            return new AuthorizationCodeInstalledApp(flow, receiver, DesktopUtils::browseUrl).authorize(id);
        } catch (StreamCorruptedException e) {
            // Data store is corrupt, so we're going to purge it and try again
            Utils.getLogger().log(Level.SEVERE, "Data store is corrupt, purging", e);

            File dataDir = dataStoreFactory.getDataDirectory();
            if (dataDir != null && dataDir.exists()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Utils.getLogger().log(Level.SEVERE, "Failed to delete " + file.getAbsolutePath());
                        }
                    }
                }
            }

            return getOAuthCredential(id);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to get OAuth authorization", e);
            throw new MicrosoftAuthException(OAUTH, "Failed to get OAuth authorization", e);
        }
    }

    public void stopReceiver() {
        try {
            receiver.stop();
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to stop receiver", e);
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

    private Entitlements getEntitlements(XboxMinecraftResponse xboxMinecraftResponse) throws MicrosoftAuthException {
        HttpRequest request = buildGetRequest(String.format("%s?requestId=%s", MINECRAFT_ENTITLEMENTS_URL, UUID.randomUUID()));
        String authorization = xboxMinecraftResponse.getAuthorization();
        request.setHeaders(request.getHeaders().setAuthorization(authorization));

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            return httpResponse.parseAs(Entitlements.class);
        } catch (IOException e) {
            Utils.getLogger().log(Level.SEVERE, "Failed to request entitlements.", e);
            throw new MicrosoftAuthException(REQUEST, "Failed to request entitlements.", e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean checkMinecraftOwnership(Entitlements entitlements) {
        return entitlements.items.stream().anyMatch(i -> i.name.equalsIgnoreCase("game_minecraft"))
                && entitlements.items.stream().anyMatch(i -> i.name.equalsIgnoreCase("product_minecraft"));
    }

    private MinecraftProfile getMinecraftProfile(XboxMinecraftResponse xboxMinecraftResponse) throws MicrosoftAuthException {
        HttpRequest request = buildGetRequest(MINECRAFT_PROFILE_URL);
        String authorization = xboxMinecraftResponse.getAuthorization();
        request.setHeaders(request.getHeaders().setAuthorization(authorization));

        // Profile API returns 404 when it returns a response for no purchased account
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse httpResponse = null;
        try {
            httpResponse = request.execute();
            httpResponse.setLoggingEnabled(true);

            switch (httpResponse.getStatusCode()) {
                case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
                    MinecraftError minecraftError = httpResponse.parseAs(MinecraftError.class);
                    throw new MicrosoftAuthException(NO_PROFILE, "Minecraft Account Error: " + minecraftError.error);
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
                        .setDataStoreFactory(dataStoreFactory)
                        .build();
    }

    private void updateCredentialStore(String id, Credential credential) {
        try {
            // Remove all the other credentials
            dataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID).clear();

            // Store the new one
            dataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID)
                    .set(id, new StoredCredential(credential));
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Failed to save user credential to the data store.", e);
        }
    }

    /**
     * This function ensures that the specified URL doesn't resolve to localhost/127.0.0.1.
     * If it does, it will open a browser window with instructions on how to fix the hosts file,
     * and it will throw a MicrosoftAuthException with a relevant error message.
     */
    private void ensureHostIsNotLocalhost(GenericUrl url) throws MicrosoftAuthException {
        try {
            if (InetAddress.getByName(url.getHost()).getHostAddress().equals("127.0.0.1")) {
                DesktopUtils.browseUrl("https://support.technicpack.net/hc/en-us/articles/18587630118541");
                throw new MicrosoftAuthException(DNS, url.getHost() + " resolves to 127.0.0.1\n\nYour hosts file needs to be fixed.");
            }
        } catch (UnknownHostException e) {
            throw new MicrosoftAuthException(DNS, "DNS resolution failed for " + url.getHost() + ": unknown host");
        }
    }

    private HttpRequest buildGetRequest(String url) throws MicrosoftAuthException {
        GenericUrl genericUrl = new GenericUrl(url);
        ensureHostIsNotLocalhost(genericUrl);

        try {
            return REQUEST_FACTORY.buildGetRequest(genericUrl);
        } catch (IOException e) {
            throw new MicrosoftAuthException(REQUEST, "Failed to build get request.", e);
        }
    }

    private HttpRequest buildPostRequest(String url, HttpContent httpContent) throws MicrosoftAuthException {
        GenericUrl genericUrl = new GenericUrl(url);
        ensureHostIsNotLocalhost(genericUrl);

        try {
            return REQUEST_FACTORY.buildPostRequest(genericUrl, httpContent);
        } catch (IOException e) {
            throw new MicrosoftAuthException(REQUEST, "Failed to build post request.", e);
        }
    }
}
