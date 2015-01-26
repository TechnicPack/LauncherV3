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

public class CacheDeleteException extends IOException {
    private Throwable cause;
    String filePath;
    private static final long serialVersionUID = 6462027370292375448L;

    public CacheDeleteException(String filePath) {
        this.filePath = filePath;
    }

    public CacheDeleteException(String filePath, Throwable cause) {
        this.filePath = filePath;
        this.cause = cause;
    }

    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public synchronized Throwable getCause() {
        return this.cause;
    }

    @Override
    public String getMessage() {
        return "An error occurred while attempting to delete '" + filePath + "' from the cache:";
    }
}
