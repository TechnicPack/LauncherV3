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

import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Users {
	private String clientToken = UUID.randomUUID().toString();
	private Map<String, User> savedUsers = new HashMap<String, User>();
	private String lastUser;

	public static Users load() {
		File users = new File(Utils.getSettingsDirectory(), "users.json");
		if (!users.exists()) {
			Utils.getLogger().log(Level.WARNING, "Unable to load users from " + users + " because it does not exist.");
			return null;
		}

		try {
			String json = FileUtils.readFileToString(users, Charset.forName("UTF-8"));
			return Utils.getGson().fromJson(json, Users.class);
		} catch (JsonSyntaxException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to load users from " + users);
			return null;
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to load users from " + users);
			return null;
		}
	}

	public void save() {
		File users = new File(Utils.getSettingsDirectory(), "users.json");
		String json = Utils.getGson().toJson(this);

		try {
			FileUtils.writeStringToFile(users, json, Charset.forName("UTF-8"));
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to save users " + users);
		}
	}

	public void addUser(User user) {
		savedUsers.put(user.getUsername(), user);
		save();
	}

	public void removeUser(String username) {
		savedUsers.remove(username);
		save();
	}

	public User getUser(String accountName) {
		return savedUsers.get(accountName);
	}

	public String getClientToken() {
		return clientToken;
	}

	public Collection<String> getUsers() {
		return savedUsers.keySet();
	}

	public Collection<User> getSavedUsers() {
		return savedUsers.values();
	}

	public void setLastUser(String lastUser) {
		this.lastUser = lastUser;
		save();
	}

	public String getLastUser() {
		return lastUser;
	}
}
