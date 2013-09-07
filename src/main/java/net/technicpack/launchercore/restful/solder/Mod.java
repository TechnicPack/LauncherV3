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

package net.technicpack.launchercore.restful.solder;

public class Mod {
	private String name;
	private String version;
	private String url;
	private String md5;

	public Mod() {

	}

	public Mod(String name, String version, String url, String md5) {
		this.name = name;
		this.version = version;
		this.url = url;
		this.md5 = md5;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getUrl() {
		return url;
	}

	public String getMd5() {
		return md5;
	}

	@Override
	public String toString() {
		return "Mod{" +
				"name='" + name + '\'' +
				", version='" + version + '\'' +
				", url='" + url + '\'' +
				", md5='" + md5 + '\'' +
				'}';
	}
}
