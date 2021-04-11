package net.technicpack.minecraftcore.microsoft.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.microsoft.auth.model.MinecraftProfile;
import net.technicpack.minecraftcore.microsoft.auth.model.XboxMinecraftResponse;

public class MicrosoftUser implements IUserType {
    public static final String MICROSOFT_USER_TYPE = "microsoft";
    private String id;
    private String username;
    private String accessToken;

    public MicrosoftUser(String id, String username, String accessToken) {
        this.id = id;
        this.username = username;
        this.accessToken = accessToken;
    }
    public MicrosoftUser(XboxMinecraftResponse authResponse, MinecraftProfile profile) {
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

}
