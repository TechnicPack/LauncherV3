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

package net.technicpack.minecraftcore.mojang.auth.response;

import net.technicpack.launchercore.auth.IAuthResponse;
import net.technicpack.minecraftcore.mojang.auth.io.Profile;
import net.technicpack.minecraftcore.mojang.auth.io.User;

import java.util.Arrays;

@SuppressWarnings({"unused"})
public class AuthResponse extends Response implements IAuthResponse {
    private String accessToken;
    private String clientToken;
    private Profile[] availableProfiles;
    private Profile selectedProfile;
    private User user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public Profile[] getAvailableProfiles() {
        return availableProfiles;
    }

    public Profile getSelectedProfile() {
        return selectedProfile;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getError() {
        String error = super.getError();

        if (this.availableProfiles != null && this.availableProfiles.length == 0 && (error == null || error.isEmpty())) {
            return "No Minecraft License";
        } else {
            return error;
        }
    }

    @Override
    public String getErrorMessage() {
        String message = super.getErrorMessage();

        if (this.availableProfiles != null && this.availableProfiles.length == 0 && (message == null || message.isEmpty())) {
            return "This Mojang account has no purchased copies of Minecraft attached.";
        } else {
            return message;
        }
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", clientToken='" + clientToken + '\'' +
                ", availableProfiles=" + Arrays.toString(availableProfiles) +
                ", selectedProfile=" + selectedProfile +
                '}';
    }
}
