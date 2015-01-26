/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.solder.http;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;

public class HttpSolderPackApi implements ISolderPackApi {

    private String baseUrl;
    private String modpackSlug;
    private String clientId;
    private String userDisplayName;
    private String mirrorUrl;

    protected HttpSolderPackApi(String baseUrl, String modpackSlug, String clientId, String userDisplayName, String mirrorUrl) throws RestfulAPIException {
        this.baseUrl = baseUrl;
        this.modpackSlug = modpackSlug;
        this.clientId = clientId;
        this.userDisplayName = userDisplayName;
        this.mirrorUrl = mirrorUrl;

        if (mirrorUrl == null)
            throw new RestfulAPIException("A mirror URL could not be retrieved from '" + baseUrl + "modpack/'");
    }

    @Override
    public String getMirrorUrl() {
        return mirrorUrl;
    }

    @Override
    public SolderPackInfo getPackInfoForBulk() throws RestfulAPIException {
        return getPackInfo();
    }

    @Override
    public SolderPackInfo getPackInfo() throws RestfulAPIException {
        String packUrl = baseUrl + "modpack/" + modpackSlug + "/?cid=" + clientId + "&u=" + userDisplayName;
        SolderPackInfo info = RestObject.getRestObject(SolderPackInfo.class, packUrl);
        info.setSolder(this);
        return info;
    }

    @Override
    public Modpack getPackBuild(String build) throws BuildInaccessibleException {
        try {
            String url = baseUrl + "modpack/" + modpackSlug + "/" + build + "/?cid=" + clientId + "&u=" + userDisplayName;
            Modpack pack = RestObject.getRestObject(Modpack.class, url);

            if (pack != null) {
                return pack;
            }
        } catch (RestfulAPIException e) {
            throw new BuildInaccessibleException(modpackSlug, build, e);
        }

        throw new BuildInaccessibleException(modpackSlug, build);
    }
}
