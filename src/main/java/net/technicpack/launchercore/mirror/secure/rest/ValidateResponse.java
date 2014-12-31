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

package net.technicpack.launchercore.mirror.secure.rest;

import net.technicpack.rest.RestObject;

public class ValidateResponse extends RestObject {
    private boolean valid;
    private String message; //error message
    private String clientToken;
    private String accessToken;
    private String downloadToken;

    public ValidateResponse() {
    }

    public ValidateResponse(String errorMessage) {
        this.valid = false;
        this.message = errorMessage;
    }

    public boolean wasValid() {
        return valid;
    }

    public String getErrorMessage() {
        return message;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDownloadToken() {
        return downloadToken;
    }
}
