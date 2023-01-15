package net.technicpack.minecraftcore.microsoft.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;

public class MicrosoftUser implements IUserType {
    public static final String MC_MS_USER_TYPE = "msa";

    private String id;
    private String username;
    private String accessToken = "0";

    public MicrosoftUser() {
    }

    public MicrosoftUser(XboxMinecraftResponse authResponse, MinecraftProfile profile) {
        this();
        this.id = profile.id;
        this.username = profile.name;
        updateAuthToken(authResponse);
    }

    @Override
    public String getUserType() {
        return MC_MS_USER_TYPE;
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

    @Override
    public String getSessionId() {
        return "token:" + accessToken + ":" + getId();
    }

    @Override
    public String getMCUserType() {
        return MC_MS_USER_TYPE;
    }

    @Override
    public String getUserProperties() {
        return "{}";
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public void login(UserModel userModel) throws AuthenticationException {
        userModel.getMicrosoftAuthenticator().refreshSession(this);
    }

    public void updateAuthToken(XboxMinecraftResponse authResponse) {
        this.accessToken = authResponse.accessToken;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
