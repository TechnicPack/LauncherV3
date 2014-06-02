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

package net.technicpack.launchercore.exception;

import java.io.IOException;

public class AuthenticationNetworkFailureException extends IOException {
    private Throwable cause;
    private static final long serialVersionUID = 5887385045789342851L;

    public AuthenticationNetworkFailureException() {

    }

    public AuthenticationNetworkFailureException(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return "An error was raised while attempting to communicate with auth.minecraft.net.";
    }

    @Override
    public synchronized Throwable getCause() {
        return cause;
    }
}
