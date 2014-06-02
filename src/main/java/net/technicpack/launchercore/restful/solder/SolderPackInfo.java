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

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.exception.RestfulAPIException;
import net.technicpack.launchercore.install.user.User;
import net.technicpack.launchercore.restful.Modpack;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.Resource;
import net.technicpack.launchercore.restful.RestObject;

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
    private transient Solder solder;

    public SolderPackInfo() {

    }

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
    public Modpack getModpack(String build, User user) throws BuildInaccessibleException {
        try {
            Modpack pack = RestObject.getRestObject(Modpack.class, SolderConstants.getSolderBuildUrl(solder.getUrl(), name, build, user.getProfile().getName()));

            if (pack != null) {
                return pack;
            }
        } catch (RestfulAPIException e) {
            throw new BuildInaccessibleException(display_name, build, e);
        }

        throw new BuildInaccessibleException(display_name, build);
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

    public static SolderPackInfo getSolderPackInfo(String url) throws RestfulAPIException {
        SolderPackInfo info = getRestObject(SolderPackInfo.class, url);
        if (info == null) {
            return null;
        }
        String solderUrl = url.substring(0, url.length() - info.getName().length() - 1);
        Solder solder = RestObject.getRestObject(Solder.class, solderUrl);
        if (solder != null) {
            solder.setUrl(solderUrl.replace("modpack/", ""));
            info.setSolder(solder);
        }
        return info;
    }

    public static SolderPackInfo getSolderPackInfo(String solderUrl, String name, User user) throws RestfulAPIException {
        SolderPackInfo info = getRestObject(SolderPackInfo.class, SolderConstants.getSolderPackInfoUrl(solderUrl, name, user.getProfile().getName()));
        Solder solder = RestObject.getRestObject(Solder.class, solderUrl + "modpack/");
        solder.setUrl(solderUrl);
        info.setSolder(solder);
        return info;
    }
}
