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

    private final String baseUrl;
    private final String modpackSlug;
    private final String clientId;
    private final String mirrorUrl;

    protected HttpSolderPackApi(String baseUrl, String modpackSlug, String clientId, String mirrorUrl) throws RestfulAPIException {
        if (baseUrl == null) {
            throw new RestfulAPIException(String.format("The Solder base URL for the modpack \"%s\" is null",
                                                        modpackSlug));
        }

        if (mirrorUrl == null) {
            throw new RestfulAPIException(String.format("The Solder mirror URL for the modpack \"%s\" is null",
                                                        modpackSlug));
        }

        // Remove the right trailing slash from the base URL so we can format the URLs in a much cleaner manner
        if (baseUrl.charAt(baseUrl.length() - 1) == '/') {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
        this.modpackSlug = modpackSlug;
        this.clientId = clientId;
        this.mirrorUrl = mirrorUrl;
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
        String packUrl = String.format("%s/modpack/%s?cid=%s", baseUrl, modpackSlug, clientId);
        SolderPackInfo info = RestObject.getRestObject(SolderPackInfo.class, packUrl);
        info.setSolder(this);
        return info;
    }

    @Override
    public Modpack getPackBuild(String build) throws BuildInaccessibleException {
        String url = String.format("%s/modpack/%s/%s?cid=%s", baseUrl, modpackSlug, build, clientId);

        try {
            return RestObject.getRestObject(Modpack.class, url);
        } catch (RestfulAPIException e) {
            throw new BuildInaccessibleException(modpackSlug, build, e);
        }
    }
}
