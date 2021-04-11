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
import net.technicpack.minecraftcore.microsoft.auth.model.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 *
 */
public class MicrosoftAuthenticator {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private static HttpRequestFactory requestFactory;

    // OAUTH
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".msauthtest/auth");
    private static final String SCOPE = "XboxLive.signin";
    private static final String TOKEN_SERVER_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String AUTHORIZATION_SERVER_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?prompt=select_account";
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    // XBOX
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    // MINECRAFT
    private static final String MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public MicrosoftAuthenticator() throws IOException {
        DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
    }

    public Credential getOAuthCredential(String username) throws IOException {
        AuthorizationCodeFlow flow =
            new AuthorizationCodeFlow.Builder(
                    BearerToken.authorizationHeaderAccessMethod(),
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    new GenericUrl(TOKEN_SERVER_URL),
                    new ClientParametersAuthentication(
                            "5f8b309f-ad5f-49bf-877a-8b94afd75b9f",
                            ""),
                    "5f8b309f-ad5f-49bf-877a-8b94afd75b9f",
                    AUTHORIZATION_SERVER_URL)
                    .setScopes(Collections.singletonList(SCOPE))
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .build();
        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setHost("localhost")
                        .setPort(8080)
                        .setCallbackPath("/")
                        .build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(username);
    }

    public void authenticateOAuth(Credential credential) {
        requestFactory = HTTP_TRANSPORT.createRequestFactory(
                request -> {
                    credential.initialize(request);
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
        );
    }

    public XboxResponse authenticateXbox(Credential credential) throws IOException {
        XboxAuthRequest xboxAuthRequest = new XboxAuthRequest(credential.getAccessToken());

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxAuthRequest);
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(XBOX_AUTH_URL), httpContent);
        return request.execute().parseAs(XboxResponse.class);
    }

    public XboxResponse authenticateXSTS(XboxResponse xboxResponse) throws IOException {
        XSTSRequest xstsRequest = new XSTSRequest(xboxResponse.token);
//        System.out.println(JSON_FACTORY.toPrettyString(xstsRequest));

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xstsRequest);
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(XBOX_XSTS_URL), httpContent);
        return request.execute().parseAs(XboxResponse.class);
    }

    public XboxMinecraftResponse authenticateMinecraftXbox(XboxResponse xstsResponse) throws IOException {
        XboxMinecraftRequest xboxMinecraftRequest = new XboxMinecraftRequest(xstsResponse);
//        System.out.println(JSON_FACTORY.toPrettyString(xboxMinecraftRequest));

        HttpContent httpContent = new JsonHttpContent(JSON_FACTORY, xboxMinecraftRequest);
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(MINECRAFT_AUTH_URL), httpContent);
        return request.execute().parseAs(XboxMinecraftResponse.class);
    }

    public MinecraftProfile getMinecraftProfile(XboxMinecraftResponse xboxMinecraftResponse) throws IOException {

        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(MINECRAFT_PROFILE_URL));
        request.setInterceptor(null); // It still had the OAuth interceptor that overwrites the authorization
        String authorization = xboxMinecraftResponse.getAuthorization();
        request.setHeaders(request.getHeaders().setAuthorization(authorization));

        return request.execute().parseAs(MinecraftProfile.class);
    }

    public void updateCredentialStore(String username, Credential credential) throws IOException {
        DATA_STORE_FACTORY.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID)
                .set(username, new StoredCredential(credential));
    }
}
