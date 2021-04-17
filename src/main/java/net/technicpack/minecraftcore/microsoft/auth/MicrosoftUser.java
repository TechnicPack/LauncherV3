package net.technicpack.minecraftcore.microsoft.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;

import java.io.IOException;

public class MicrosoftUser implements IUserType {
    public static final String MICROSOFT_USER_TYPE = "microsoft";
    private String id;
    private String username;
    private String accessToken;

    private final MicrosoftAuthenticator microsoftAuthenticator;

    public MicrosoftUser() {
        this.microsoftAuthenticator = new MicrosoftAuthenticator();
    }

    public MicrosoftUser(XboxMinecraftResponse authResponse, MinecraftProfile profile) {
        this();
        this.id = profile.id;
        this.username = profile.name;
        this.accessToken = authResponse.accessToken;
    }

    @Override
    public String getUserType() {
        return MICROSOFT_USER_TYPE;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return username;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }
    public String getSessionId() {
        return "token:" + accessToken + ":" + getId();
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    private void refreshToken(XboxMinecraftResponse authResponse) {
        this.accessToken = authResponse.accessToken;
    }

    @Override
    public void login() throws AuthenticationException {
        try {
            refreshToken(microsoftAuthenticator.getAuthTokenFromUsername(getUsername()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
