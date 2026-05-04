package net.technicpack.minecraftcore.microsoft.auth;

import static net.technicpack.launchercore.exception.MicrosoftAuthException.ExceptionType.*;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.*;
import com.google.api.client.http.apache.v5.Apache5HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import io.sentry.Sentry;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.model.*;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

public class MicrosoftAuthenticator {
  private static final HttpTransport HTTP_TRANSPORT = new Apache5HttpTransport();
  private static final JsonFactory JSON_FACTORY = new GsonFactory();
  private final HttpRequestFactory REQUEST_FACTORY;

  // OAUTH
  private static final String TECHNIC_CLIENT_ID = "8dfabc1d-38a9-42d8-bc08-677dbc60fe65";
  private static final String[] SCOPES = {"XboxLive.signin", "XboxLive.offline_access"};
  private static final String TOKEN_SERVER_URL =
      "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
  private static final String AUTHORIZATION_SERVER_URL =
      "https://login.live.com/oauth20_authorize.srf?prompt=select_account&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d";
  private static final String DEVICE_CODE_URL =
      "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
  private static final String DEVICE_CODE_GRANT_TYPE =
      "urn:ietf:params:oauth:grant-type:device_code";
  private final DataStoreFactory dataStoreFactory;

  /**
   * Non-null when the on-disk credential store at this path could not be initialised (usually
   * Windows ACL corruption), and we fell back to an in-memory store. Callers should surface a
   * warning to the user telling them how to clean up the folder manually; in-memory mode means the
   * user has to sign in every launch until the path is resolvable again.
   */
  private final String corruptedCredentialStorePath;

  // XBOX
  private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
  private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

  // MINECRAFT
  private static final String MINECRAFT_AUTH_URL =
      "https://api.minecraftservices.com/launcher/login";
  private static final String MINECRAFT_PROFILE_URL =
      "https://api.minecraftservices.com/minecraft/profile";
  private static final String MINECRAFT_ENTITLEMENTS_URL =
      "https://api.minecraftservices.com/entitlements/license";

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
    REQUEST_FACTORY =
        HTTP_TRANSPORT.createRequestFactory(
            request -> {
              request.setParser(new JsonObjectParser(JSON_FACTORY));

              request.setLoggingEnabled(true);
              request.setCurlLoggingEnabled(true);
            });

    // Work around the Access Denied exception that sometimes happens due to Windows ACL permission
    // issues. FileDataStoreFactory probes ownership of the datastore folder on init, and fails
    // hard if it can't read it. If the folder is in a corrupt ACL state (stale owner, permissions
    // rewritten by a cleaner, profile SID changed after a reinstall), try to reset it. If every
    // recovery strategy fails, fall back to an in-memory credential store so the launcher can
    // still run: the user has to sign in each launch until they fix the folder by hand, but the
    // launcher does not crash. The caller is expected to check getCorruptedCredentialStorePath()
    // and surface a warning dialog explaining how to clean up.
    Path dataStorePath = Paths.get(dataStore.getAbsolutePath());
    DataStoreFactory factory = null;
    String corruptedPath = null;

    if (Files.exists(dataStorePath)) {
      try {
        FileOwnerAttributeView fileAttributeView =
            Files.getFileAttributeView(dataStorePath, FileOwnerAttributeView.class);
        fileAttributeView.getOwner();
      } catch (AccessDeniedException e) {
        if (!resetCorruptedDataStore(dataStorePath)) {
          Sentry.captureException(e);
          Utils.getLogger()
              .log(
                  Level.WARNING,
                  "Credential store at "
                      + dataStorePath
                      + " is in a corrupt permission state and could not be reset. "
                      + "Falling back to an in-memory credential store.",
                  e);
          factory = MemoryDataStoreFactory.getDefaultInstance();
          corruptedPath = dataStorePath.toString();
        }
      } catch (IOException e) {
        Sentry.captureException(e);
        // Ignore other IO exceptions; FileDataStoreFactory will either succeed or fall back below.
      }
    }

    if (factory == null) {
      try {
        factory = new FileDataStoreFactory(dataStore);
      } catch (IOException e) {
        Sentry.captureException(e);
        Utils.getLogger()
            .log(
                Level.WARNING,
                "Failed to create file-backed credential store at "
                    + dataStorePath
                    + ". Falling back to an in-memory credential store.",
                e);
        factory = MemoryDataStoreFactory.getDefaultInstance();
        corruptedPath = dataStorePath.toString();
      }
    }

    this.dataStoreFactory = factory;
    this.corruptedCredentialStorePath = corruptedPath;
  }

  /**
   * Returns the on-disk path that the credential store could not use, or null if the credential
   * store is healthy (on-disk, persistent). When non-null, the launcher is running with an
   * in-memory credential store: the user has to sign in every launch until this folder is cleaned
   * up manually. Callers should surface a warning dialog with cleanup guidance.
   */
  public String getCorruptedCredentialStorePath() {
    return corruptedCredentialStorePath;
  }

  /**
   * Try progressively harder strategies to clear a credential-store folder that is in a corrupt
   * permission state. Returns true if the folder is now absent (so FileDataStoreFactory can create
   * a fresh one) or false if every strategy failed.
   */
  static boolean resetCorruptedDataStore(Path dataStorePath) {
    // Strategy 1: recursive delete. Handles both empty and non-empty folders, and succeeds when
    // the folder contents are accessible but the folder itself has stale ownership.
    try {
      FileUtils.deleteDirectory(dataStorePath.toFile());
      return true;
    } catch (IOException e) {
      Utils.getLogger().log(Level.WARNING, "Recursive delete of " + dataStorePath + " failed", e);
    }

    // Strategy 2: rename the folder aside. Windows file rename uses different NTFS permission
    // bits than delete in some configurations, so rename can succeed where delete does not.
    // FileDataStoreFactory will then create a fresh empty folder at the original path.
    Path renamed =
        dataStorePath.resolveSibling(
            dataStorePath.getFileName() + ".corrupted." + System.currentTimeMillis());
    try {
      Files.move(dataStorePath, renamed);
      Utils.getLogger()
          .log(
              Level.WARNING,
              "Renamed corrupted credential store to " + renamed + "; creating fresh folder");
      return true;
    } catch (IOException e) {
      Utils.getLogger()
          .log(Level.WARNING, "Rename of " + dataStorePath + " to " + renamed + " failed", e);
    }

    return false;
  }

  public MicrosoftUser loginNewUser() throws MicrosoftAuthException {
    final String tempCredentialId = UUID.randomUUID().toString();
    Credential credential = getOAuthCredential(tempCredentialId);
    return completeMinecraftLogin(credential);
  }

  /**
   * Device-code equivalent of {@link #loginNewUser()}: acquires a Microsoft OAuth token without the
   * LocalServerReceiver / localhost-callback path. Useful when the user cannot or will not get the
   * browser redirect back to the launcher (restrictive firewalls, corporate proxies, cancelled
   * Windows Defender prompts, browser sign-in on a different device).
   *
   * @param listener receives the device-code challenge (user code + verification URL) as soon as
   *     Microsoft hands it back, so the caller's UI can render it and the user can act on it.
   * @param cancelled polled between polling iterations; if it ever returns true, the method aborts
   *     with a MicrosoftAuthException.
   */
  public MicrosoftUser loginNewUserViaDeviceCode(
      DeviceCodeListener listener, BooleanSupplier cancelled) throws MicrosoftAuthException {
    final String tempCredentialId = UUID.randomUUID().toString();

    DeviceCodeResponse challenge;
    try {
      challenge = requestDeviceCode();
    } catch (IOException e) {
      throw new MicrosoftAuthException(OAUTH, "Failed to start device code sign-in", e);
    }

    listener.onChallengeReady(
        new DeviceCodeChallenge(
            challenge.userCode,
            challenge.verificationUri,
            challenge.verificationUriComplete,
            challenge.expiresIn));

    Credential credential;
    try {
      credential =
          pollDeviceCodeToken(
              challenge.deviceCode,
              Math.max(1, challenge.interval),
              challenge.expiresIn,
              tempCredentialId,
              cancelled);
    } catch (IOException e) {
      throw new MicrosoftAuthException(OAUTH, "Device code polling failed", e);
    }

    return completeMinecraftLogin(credential);
  }

  /** Shared post-credential pipeline: Xbox auth, Minecraft entitlement check, profile, store. */
  private MicrosoftUser completeMinecraftLogin(Credential credential)
      throws MicrosoftAuthException {
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

  private DeviceCodeResponse requestDeviceCode() throws IOException {
    Map<String, String> params = new HashMap<>();
    params.put("client_id", TECHNIC_CLIENT_ID);
    params.put("scope", String.join(" ", SCOPES));

    HttpRequest request =
        REQUEST_FACTORY.buildPostRequest(
            new GenericUrl(DEVICE_CODE_URL), new UrlEncodedContent(params));

    HttpResponse response = request.execute();
    try {
      return response.parseAs(DeviceCodeResponse.class);
    } finally {
      try {
        response.disconnect();
      } catch (IOException ignored) {
        // disconnect-time IO noise is not worth propagating
      }
    }
  }

  private Credential pollDeviceCodeToken(
      String deviceCode,
      int intervalSeconds,
      long expiresInSeconds,
      String tempCredentialId,
      BooleanSupplier cancelled)
      throws IOException, MicrosoftAuthException {
    final long deadlineMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L);
    int currentInterval = intervalSeconds;

    while (true) {
      if (cancelled.getAsBoolean()) {
        throw new MicrosoftAuthException(OAUTH, "Device code sign-in was cancelled.");
      }
      if (System.currentTimeMillis() > deadlineMillis) {
        throw new MicrosoftAuthException(
            OAUTH, "Device code expired before sign-in was completed.");
      }

      try {
        Thread.sleep(currentInterval * 1000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MicrosoftAuthException(OAUTH, "Device code polling was interrupted.", e);
      }

      if (cancelled.getAsBoolean()) {
        throw new MicrosoftAuthException(OAUTH, "Device code sign-in was cancelled.");
      }

      Map<String, String> params = new HashMap<>();
      params.put("grant_type", DEVICE_CODE_GRANT_TYPE);
      params.put("client_id", TECHNIC_CLIENT_ID);
      params.put("device_code", deviceCode);

      HttpRequest request =
          REQUEST_FACTORY.buildPostRequest(
              new GenericUrl(TOKEN_SERVER_URL), new UrlEncodedContent(params));
      // 400 with error=authorization_pending is the expected steady state. Don't throw on it.
      request.setThrowExceptionOnExecuteError(false);

      HttpResponse response = request.execute();
      DevicePollResponse parsed;
      try {
        parsed = response.parseAs(DevicePollResponse.class);
      } finally {
        try {
          response.disconnect();
        } catch (IOException ignored) {
          // ignore
        }
      }

      if (parsed.accessToken != null && !parsed.accessToken.isEmpty()) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(parsed.accessToken);
        tokenResponse.setRefreshToken(parsed.refreshToken);
        tokenResponse.setExpiresInSeconds(parsed.expiresIn);
        tokenResponse.setTokenType(parsed.tokenType);
        tokenResponse.setScope(parsed.scope);
        try {
          return buildFlow().createAndStoreCredential(tokenResponse, tempCredentialId);
        } catch (IOException e) {
          throw new MicrosoftAuthException(OAUTH, "Failed to store device code credential", e);
        }
      }

      String error = parsed.error == null ? "" : parsed.error;
      switch (error) {
        case "authorization_pending":
          // User has not yet completed sign-in. Keep polling at current interval.
          break;
        case "slow_down":
          // Microsoft asked us to back off. Add 5s to the interval permanently per RFC 8628.
          currentInterval += 5;
          break;
        case "authorization_declined":
          throw new MicrosoftAuthException(OAUTH, "User declined the sign-in request.");
        case "expired_token":
          throw new MicrosoftAuthException(
              OAUTH, "Device code expired before sign-in was completed.");
        case "bad_verification_code":
          throw new MicrosoftAuthException(
              OAUTH, "Device code rejected: " + parsed.errorDescription);
        default:
          throw new MicrosoftAuthException(
              OAUTH,
              "Device code polling failed: "
                  + error
                  + (parsed.errorDescription == null ? "" : " " + parsed.errorDescription));
      }
    }
  }

  /** Public view of the device-code challenge passed to the UI via {@link DeviceCodeListener}. */
  public static final class DeviceCodeChallenge {
    public final String userCode;
    public final String verificationUri;

    /** May be null on endpoints that do not return a pre-filled URL (e.g. the consumer tenant). */
    public final String verificationUriComplete;

    public final long expiresInSeconds;

    DeviceCodeChallenge(
        String userCode,
        String verificationUri,
        String verificationUriComplete,
        long expiresInSeconds) {
      this.userCode = userCode;
      this.verificationUri = verificationUri;
      this.verificationUriComplete = verificationUriComplete;
      this.expiresInSeconds = expiresInSeconds;
    }
  }

  /** Called once by {@link #loginNewUserViaDeviceCode} as soon as the challenge is available. */
  public interface DeviceCodeListener {
    void onChallengeReady(DeviceCodeChallenge challenge);
  }

  /** JSON wire shape of the /devicecode response. */
  public static class DeviceCodeResponse {
    @Key("device_code")
    String deviceCode;

    @Key("user_code")
    String userCode;

    @Key("verification_uri")
    String verificationUri;

    @Key("verification_uri_complete")
    String verificationUriComplete;

    @Key("expires_in")
    long expiresIn;

    @Key int interval;
  }

  /** JSON wire shape of /token polling responses, covering both success and pending-error cases. */
  public static class DevicePollResponse {
    @Key("access_token")
    String accessToken;

    @Key("refresh_token")
    String refreshToken;

    @Key("expires_in")
    Long expiresIn;

    @Key("token_type")
    String tokenType;

    @Key String scope;

    @Key String error;

    @Key("error_description")
    String errorDescription;
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
      final Long expiresInSeconds = credential.getExpiresInSeconds();
      final boolean shouldRefresh = expiresInSeconds == null || expiresInSeconds <= 86400;
      if (shouldRefresh && !credential.refreshToken()) {
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
    receiver =
        new LocalServerReceiver.Builder()
            .setHost("localhost")
            .setPort(-1)
            .setCallbackPath("/")
            .build();

    try {
      AuthorizationCodeFlow flow = buildFlow();
      return new AuthorizationCodeInstalledApp(flow, receiver, DesktopUtils::browseUrl)
          .authorize(id);
    } catch (StreamCorruptedException e) {
      // Data store is corrupt, so we're going to purge it and try again. Only relevant when the
      // data store is file-backed (in-memory doesn't persist anything that could get corrupted).
      Utils.getLogger().log(Level.SEVERE, "Data store is corrupt, purging", e);

      if (dataStoreFactory instanceof FileDataStoreFactory) {
        File dataDir = ((FileDataStoreFactory) dataStoreFactory).getDataDirectory();
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
          throw new MicrosoftAuthException(
              unauthorized.getExceptionType(), "Failed to get XSTS authentication.");
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

  private XboxMinecraftResponse authenticateMinecraftXbox(XboxResponse xstsResponse)
      throws MicrosoftAuthException {
    XboxMinecraftRequest xboxMinecraftRequest = new XboxMinecraftRequest(xstsResponse);

    HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxMinecraftRequest);
    HttpRequest request = buildPostRequest(MINECRAFT_AUTH_URL, httpContent);

    HttpResponse httpResponse = null;
    try {
      httpResponse = request.execute();
      return httpResponse.parseAs(XboxMinecraftResponse.class);
    } catch (IOException e) {
      Utils.getLogger().log(Level.SEVERE, "Failed to authenticate with Xbox for Minecraft.", e);
      throw new MicrosoftAuthException(
          XBOX_MINECRAFT, "Failed to authenticate with Xbox for Minecraft.", e);
    } finally {
      if (httpResponse != null) {
        try {
          httpResponse.disconnect();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private Entitlements getEntitlements(XboxMinecraftResponse xboxMinecraftResponse)
      throws MicrosoftAuthException {
    HttpRequest request =
        buildGetRequest(
            String.format("%s?requestId=%s", MINECRAFT_ENTITLEMENTS_URL, UUID.randomUUID()));
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

  private MinecraftProfile getMinecraftProfile(XboxMinecraftResponse xboxMinecraftResponse)
      throws MicrosoftAuthException {
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
          throw new MicrosoftAuthException(
              NO_PROFILE, "Minecraft Account Error: " + minecraftError.error);
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
      dataStoreFactory
          .getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID)
          .set(id, new StoredCredential(credential));
    } catch (IOException e) {
      Utils.getLogger().log(Level.WARNING, "Failed to save user credential to the data store.", e);
    }
  }

  /**
   * This function ensures that the specified URL doesn't resolve to localhost/127.0.0.1. If it
   * does, it will open a browser window with instructions on how to fix the hosts file, and it will
   * throw a MicrosoftAuthException with a relevant error message.
   */
  private void ensureHostIsNotLocalhost(GenericUrl url) throws MicrosoftAuthException {
    try {
      if (InetAddress.getByName(url.getHost()).getHostAddress().equals("127.0.0.1")) {
        DesktopUtils.browseUrl("https://support.technicpack.net/hc/en-us/articles/18587630118541");
        throw new MicrosoftAuthException(
            DNS, url.getHost() + " resolves to 127.0.0.1\n\nYour hosts file needs to be fixed.");
      }
    } catch (UnknownHostException e) {
      throw new MicrosoftAuthException(
          DNS, "DNS resolution failed for " + url.getHost() + ": unknown host");
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

  private HttpRequest buildPostRequest(String url, HttpContent httpContent)
      throws MicrosoftAuthException {
    GenericUrl genericUrl = new GenericUrl(url);
    ensureHostIsNotLocalhost(genericUrl);

    try {
      return REQUEST_FACTORY.buildPostRequest(genericUrl, httpContent);
    } catch (IOException e) {
      throw new MicrosoftAuthException(REQUEST, "Failed to build post request.", e);
    }
  }
}
