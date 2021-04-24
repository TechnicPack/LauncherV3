package net.technicpack.minecraftcore.microsoft.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxResponse;

import java.time.Instant;

public class MicrosoftUser implements IUserType {
    public static final String MICROSOFT_USER_TYPE = "microsoft";
    private String id;
    private String username;
    private String xboxAccessToken;
    private String accessToken;
    private long xboxTokenValidUntil;
    private long minecraftTokenValidUntil;

    public MicrosoftUser() {
    }

    public MicrosoftUser(XboxResponse xboxResponse,
                         XboxMinecraftResponse authResponse, MinecraftProfile profile) {
        this();
        this.id = profile.id;
        this.username = profile.name;
        this.accessToken = authResponse.accessToken;
        this.xboxAccessToken = xboxResponse.token;
        this.xboxTokenValidUntil = Instant.parse(xboxResponse.notAfter).getEpochSecond();
        this.minecraftTokenValidUntil = (System.currentTimeMillis() / 1000) + authResponse.expiresIn;
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

    public String getXboxAccessToken() {
        return xboxAccessToken;
    }

    private void refreshToken(XboxMinecraftResponse authResponse) {
        this.accessToken = authResponse.accessToken;
    }

    public long getXboxExpiresInSeconds() {
        return xboxTokenValidUntil - (System.currentTimeMillis() / 1000);
    }

    public long getMinecraftExpiresInSeconds() {
        return minecraftTokenValidUntil - (System.currentTimeMillis() / 1000);
    }

    @Override
    public void login(UserModel userModel) throws AuthenticationException {
        MicrosoftAuthenticator authenticator = userModel.getMicrosoftAuthenticator();
        refreshToken(authenticator.refreshSession(this));
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
