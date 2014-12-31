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

package net.technicpack.launchercore.mirror.secure;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.mirror.secure.rest.ISecureMirror;

import java.util.Date;

public class SecureToken {
    private String token;
    private Date receivedTime;
    private UserModel userModel;
    private ISecureMirror mirror;

    private String tokenUserName;
    private String tokenAccessToken;

    public SecureToken(UserModel userModel, ISecureMirror mirror) {
        this.token = null;
        this.receivedTime = null;
        this.userModel = userModel;
        this.mirror = mirror;
    }

    public String getDownloadHost() {
        return mirror.getDownloadHost();
    }

    public String queryForSecureToken() throws DownloadException {
        return null;
    }
}
