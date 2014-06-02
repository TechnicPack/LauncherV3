package net.technicpack.launchercore.mirror.secure.rest;

import net.technicpack.launchercore.exception.RestfulAPIException;
import net.technicpack.launchercore.restful.RestObject;

/**
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 * <p/>
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

public class JsonWebSecureMirror implements ISecureMirror {
    private String baseUrl;
    private String downloadHost;

    public JsonWebSecureMirror(String baseUrl, String downloadHost) {
        this.baseUrl = baseUrl;
        this.downloadHost = downloadHost;
    }

    @Override
    public String getDownloadHost() {
        return downloadHost;
    }

    @Override
    public ValidateResponse validate(ValidateRequest req) {
        String constructedUrl = baseUrl + "validate?a=" + req.getAccessToken() + "&c=" + req.getClientToken();

        try {
            return RestObject.getRestObject(ValidateResponse.class, constructedUrl);
        } catch (RestfulAPIException ex) {
            ex.printStackTrace();
            return new ValidateResponse(ex.getMessage());
        }
    }
}
