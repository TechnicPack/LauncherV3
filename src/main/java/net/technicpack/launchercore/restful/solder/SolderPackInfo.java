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

import net.technicpack.launchercore.restful.Resource;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.RestObject;

import java.util.List;

public class SolderPackInfo extends RestObject implements PackInfo {

	private String name;
	private String display_name;
	private String url;
	private String icon_md5;
	private String logo_md5;
	private String background_md5;
	private String recommended;
	private String latest;
	private List<String> builds;

	private transient Solder solder;

	public Solder getSolder() {
		return solder;
	}

	public void setSolder(Solder solder) {
		this.solder = solder;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return display_name;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public Resource getIcon() {
		return new Resource(solder.getMirrorUrl() + name + "/resources/icon.png", icon_md5);
	}

	@Override
	public Resource getBackground() {
		return new Resource(solder.getMirrorUrl() + name + "/resources/background.jpg", background_md5);
	}

	@Override
	public Resource getLogo() {
		return new Resource(solder.getMirrorUrl() + name + "/resources/logo_180.png", logo_md5);
	}

	@Override
	public String getRecommended() {
		return recommended;
	}

	@Override
	public String getLatest() {
		return latest;
	}

	public List<String> getBuilds() {
		return builds;
	}
}
