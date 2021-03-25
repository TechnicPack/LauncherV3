package net.technicpack.minecraftcore.msa.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.msa.auth.response.MinecraftProfile;

public class MsaUser implements IUserType {
    private MinecraftProfile profile;
    private String accessToken;
    private String refreshToken;

    public MsaUser(MinecraftProfile profile, String accessToken, String refreshToken) {
        this.profile = profile;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    @Override
    public String getId() {
        return profile.GetID();
    }

    @Override
    public String getUsername() {
        return profile.GetName();
    }

    @Override
    public String getDisplayName() {
        return profile.GetName();
    }

    @Override
    public String getAccessToken() { return accessToken; }

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getRefreshToken() { return refreshToken; }
}
