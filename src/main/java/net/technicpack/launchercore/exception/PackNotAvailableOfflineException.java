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

package net.technicpack.launchercore.exception;

import java.io.IOException;

public class PackNotAvailableOfflineException extends IOException {
    private String packDisplayName;
    private Throwable cause;
    private static final long serialVersionUID = 3246491999503435492L;

    public PackNotAvailableOfflineException(String displayName) {
        this.packDisplayName = displayName;
    }

    public PackNotAvailableOfflineException(String displayName, Throwable cause) {
        this.packDisplayName = displayName;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return "The modpack " + packDisplayName + " does not appear to be installed or is corrupt, and is not available for Offline Play.";
    }

    @Override
    public synchronized Throwable getCause() {
        return cause;
    }
}
