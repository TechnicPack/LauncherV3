package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

import java.util.List;

public class XboxMinecraftResponse {
    @Key(value="username") public String username;
    @Key(value="roles") public List<Object> roles;
    @Key(value="access_token") public String accessToken;
    @Key(value="token_type") public String tokenType;
    @Key(value="expires_in") public int expiresIn;

    public String getAuthorization() {
        return String.format("%s %s", tokenType, accessToken);
    }

    @Override
    public String toString() {
        return "XboxMinecraftResponse{\n" +
                "username='" + username + '\'' +
                ",\n roles=" + roles +
                ",\n accessToken='" + accessToken + '\'' +
                ",\n tokenType='" + tokenType + '\'' +
                ",\n expiresIn=" + expiresIn +
                "\n}";
    }
}
