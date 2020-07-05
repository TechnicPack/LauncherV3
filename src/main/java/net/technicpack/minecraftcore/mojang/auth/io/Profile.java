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

package net.technicpack.minecraftcore.mojang.auth.io;

@SuppressWarnings({"unused"})
public class Profile {
	private String id;
	private String name;
	private boolean legacy;

    public Profile() {

    }

    public Profile(String id, String name) {
        this.id = id;
        this.name = name;
    }

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isLegacy() {
		return legacy;
	}

	@Override
	public String toString() {
		return "Profile{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
