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

package net.technicpack.launchercore.restful.platform;

import net.technicpack.launchercore.restful.Resource;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.RestObject;

public class PlatformPackInfo extends RestObject implements PackInfo {
	private String name;
	private String displayName;
	private String url;
	private Resource icon;
	private Resource logo;
	private Resource background;
	private String minecraft;
	private String forge;
	private String build;
	private String solder;

	@Override
	public Resource getBackground() {
		return background;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Resource getIcon() {
		return icon;
	}

	@Override
	public String getLatest() {
		return build;
	}

	@Override
	public Resource getLogo() {
		return logo;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getRecommended() {
		return build;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public String getMinecraft() {
		return minecraft;
	}

	public String getForge() {
		return forge;
	}

	public String getSolder() {
		return solder;
	}

	public boolean hasSolder() {
		return solder == null || solder.equals("");
	}
}
