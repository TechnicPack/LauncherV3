package net.technicpack.minecraftcore.microsoft.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxResponse;

import java.time.Instant;

public class MicrosoftUser implements IUserType {
    public static final String MC_MS_USER_TYPE = "msa";

    private String id;
    private String username;
    private String accessToken = "0";

    private transient boolean isOffline = false;

    @SuppressWarnings("unused") // Gson constructor
    private MicrosoftUser() {}

    public MicrosoftUser(XboxMinecraftResponse authResponse, MinecraftProfile profile) {
        this.id = profile.id;
        this.username = profile.name;
        updateAuthToken(authResponse);
    }

    /**
     * Constructor for offline mode
     * @param id Minecraft player UUID
     * @param username Minecraft username
     */
    public MicrosoftUser(String id, String username) {
        this.id = id;
        this.username = username;
        this.isOffline = true;
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
        return "token:" + accessToken + ":" + id;
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
        return isOffline;
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
