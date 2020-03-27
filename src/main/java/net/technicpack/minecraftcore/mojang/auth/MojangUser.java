/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.auth;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.auth.io.Profile;
import net.technicpack.minecraftcore.mojang.auth.io.UserProperties;
import net.technicpack.minecraftcore.mojang.auth.response.AuthResponse;

public class MojangUser implements IUserType {
    private String username;
    private String accessToken;
    private String clientToken;
    private String displayName;
    private Profile profile;
    private UserProperties userProperties;
    private transient boolean isOffline;

    public MojangUser() {
        isOffline = false;
    }

    //This constructor is used to build a user for offline mode
    public MojangUser(String username) {
        this.username = username;
        this.displayName = username;
        this.accessToken = "0";
        this.clientToken = "0";
        this.profile = new Profile("0", "");
        this.isOffline = true;
        this.userProperties = new UserProperties();
    }

    public MojangUser(String username, AuthResponse response) {
        this.isOffline = false;
        this.username = username;
        this.accessToken = response.getAccessToken();
        this.clientToken = response.getClientToken();
        this.displayName = response.getSelectedProfile().getName();
        this.profile = response.getSelectedProfile();

        if (response.getUser() == null) {
            this.userProperties = new UserProperties();
        } else {
            this.userProperties = response.getUser().getUserProperties();
        }
    }

    public String getId() {
        return profile.getId();
    }

    public String getUsername() {
        return username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Profile getProfile() {
        return profile;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public String getSessionId() {
        return "token:" + accessToken + ":" + profile.getId();
    }

    public void rotateAccessToken(String newToken) {
        this.accessToken = newToken;
    }

    public String getUserPropertiesAsJson() {
        if (this.userProperties != null)
            return MojangUtils.getUglyGson().toJson(this.userProperties);
        else
            return "{}";
    }

    public void mergeUserProperties(MojangUser mergeUser) {
        if (this.userProperties != null && mergeUser.userProperties != null)
            this.userProperties.merge(mergeUser.userProperties);
    }
}
