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

package net.technicpack.solder.io;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.rest.RestObject;
import net.technicpack.solder.ISolderPackApi;

import java.util.List;

public class SolderPackInfo extends RestObject implements PackInfo {

	private String name;
	private String display_name;
	private String url;
	private String icon;
	private String icon_md5;
	private String logo;
	private String logo_md5;
	private String background;
	private String background_md5;
	private String recommended;
	private String latest;
	private List<String> builds;
	private transient ISolderPackApi solder;

	public SolderPackInfo() {

	}

	public ISolderPackApi getSolder() {
		return solder;
	}

	public void setSolder(ISolderPackApi solder) {
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
		if (icon == null) {
			return new Resource(solder.getMirrorUrl() + name + "/resources/icon.png", logo_md5);
		}
		return new Resource(icon, icon_md5);
	}

	@Override
	public Resource getBackground() {
		if (background == null) {
			return new Resource(solder.getMirrorUrl() + name + "/resources/background.jpg", logo_md5);
		}
		return new Resource(background, background_md5);
	}

	@Override
	public Resource getLogo() {
		if (logo == null) {
			return new Resource(solder.getMirrorUrl() + name + "/resources/logo_180.png", logo_md5);
		}
		return new Resource(logo, logo_md5);
	}

	@Override
	public String getRecommended() {
		return recommended;
	}

	@Override
	public String getLatest() {
		return latest;
	}

	@Override
	public List<String> getBuilds() {
		return builds;
	}

	@Override
	public boolean shouldForceDirectory() {
		//TODO: This is not really implemented properly
		return false;
	}

    @Override
    public String getDescription() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc facilisis congue dignissim. Aliquam posuere eros vel eros luctus molestie. Duis non massa vel orci sagittis semper. Pellentesque lorem diam, viverra in bibendum in, tincidunt in neque. Curabitur consectetur aliquam sem eget laoreet. Quisque eget turpis a velit semper dictum at ut neque. Nulla placerat odio eget neque commodo posuere. Nam porta lacus elit, a rutrum enim mollis vel.";
    }

	@Override
	public Modpack getModpack(String build) throws BuildInaccessibleException {
        return solder.getPackBuild(build);
	}

	@Override
	public String toString() {
		return "SolderPackInfo{" +
				"name='" + name + '\'' +
				", display_name='" + display_name + '\'' +
				", url='" + url + '\'' +
				", icon_md5='" + icon_md5 + '\'' +
				", logo_md5='" + logo_md5 + '\'' +
				", background_md5='" + background_md5 + '\'' +
				", recommended='" + recommended + '\'' +
				", latest='" + latest + '\'' +
				", builds=" + builds +
				", solder=" + solder +
				'}';
	}
}
