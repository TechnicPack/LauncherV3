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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Users {
	private UUID clientToken = UUID.randomUUID();
	private Map<String, User> savedUsers = new HashMap<String, User>();

	public void addUser(User user) {
		savedUsers.put(user.getUsername(), user);
	}

	public User getUser(String accountName) {
		return savedUsers.get(accountName);
	}

	public UUID getClientToken() {
		return clientToken;
	}

	public Collection<String> getUsers() {
		return savedUsers.keySet();
	}

	public Collection<User> getSavedUsers() {
		return savedUsers.values();
	}
}
