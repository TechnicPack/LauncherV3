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

public class BuildInaccessibleException extends IOException {
    private String packDisplayName;
    private String build;
    private Throwable cause;
    private static final long serialVersionUID = -4905270588640056830L;

    public BuildInaccessibleException(String displayName, String build) {
        this.packDisplayName = displayName;
        this.build = build;
    }

    public BuildInaccessibleException(String displayName, String build, Throwable cause) {
        this(displayName, build);
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        if (this.cause != null) {
            Throwable rootCause = this.cause;

            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            return "An error was raised while attempting to read pack info for modpack " + packDisplayName + ", build " + build + ": " + rootCause.getMessage();
        } else {
            return "The pack host returned unrecognizable garbage while attempting to read pack info for modpack " + packDisplayName + ", build " + build + ".";
        }
    }

    @Override
    public synchronized Throwable getCause() {
        return cause;
    }
}
