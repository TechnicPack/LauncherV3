/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.install.user;

import com.google.gson.JsonObject;
import net.technicpack.launchercore.auth.AuthResponse;
import net.technicpack.launchercore.auth.Profile;
import net.technicpack.launchercore.util.Utils;

public class User {
    private String username;
    private String accessToken;
    private String clientToken;
    private String displayName;
    private Profile profile;
    private JsonObject userProperties;
    private transient boolean isOffline;

    public User() {
        isOffline = false;
    }

    //This constructor is used to build a user for offline mode
    public User(String username) {
        this.username = username;
        this.displayName = username;
        this.accessToken = "0";
        this.clientToken = "0";
        this.profile = new Profile("0", "");
        this.isOffline = true;
        this.userProperties = Utils.getGson().fromJson("{}", JsonObject.class);
    }

    public User(String username, AuthResponse response) {
        this.isOffline = false;
        this.username = username;
        this.accessToken = response.getAccessToken();
        this.clientToken = response.getClientToken();
        this.displayName = response.getSelectedProfile().getName();
        this.profile = response.getSelectedProfile();

        if (response.getUser() == null) {
            this.userProperties = Utils.getGson().fromJson("{}", JsonObject.class);
        } else {
            this.userProperties = response.getUser().getUserProperties();
        }
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
        return Utils.getGson().toJson(this.userProperties);
    }
}
