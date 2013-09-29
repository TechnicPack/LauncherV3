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

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.auth.AuthResponse;
import net.technicpack.launchercore.auth.Profile;

public class User {
	private String username;
	private String accessToken;
	private String clientToken;
	private String displayName;
	private Profile profile;

	public User() {

	}

	public User(String username, AuthResponse response) {
		this.username = username;
		this.accessToken = response.getAccessToken();
		this.clientToken = response.getClientToken();
		this.displayName = response.getSelectedProfile().getName();
		this.profile = response.getSelectedProfile();
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

	public String getSessionId() {
		return "token:" + accessToken + ":" + profile.getId();
	}
}
