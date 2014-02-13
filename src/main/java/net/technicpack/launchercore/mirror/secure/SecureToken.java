package net.technicpack.launchercore.mirror.secure;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.user.UserModel;
import net.technicpack.launchercore.mirror.secure.rest.ISecureMirror;

import java.util.Date;

/**
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

public class SecureToken {
    private String token;
    private Date receivedTime;
    private UserModel userModel;
    private ISecureMirror mirror;

    public SecureToken(UserModel userModel, ISecureMirror mirror) {
        this.token = null;
        this.receivedTime = null;
        this.userModel = userModel;
        this.mirror = mirror;
    }

    public String queryForSecureToken() throws DownloadException {
        if (this.token != null && this.receivedTime != null) {
            Date now = new Date();
            long diffInMinutes = ((now.getTime() - receivedTime.getTime()) / 1000) / 60;

            if (diffInMinutes < 25)
                return this.token;
        }

        //We need to hit the mirror for a new token
        this.token = userModel.retrieveDownloadToken(this.mirror);
        this.receivedTime = new Date();

        return this.token;
    }
}
