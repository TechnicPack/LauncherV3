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

package net.technicpack.autoupdate.io;

@SuppressWarnings({"unused"})
public class LauncherResource {
    private String filename;
    private String url;
    private String md5;
    private String sha256;
    private String zstdUrl;

    protected LauncherResource() {
        // Empty constructor for GSON
    }

    public String getFilename() { return filename; }
    public String getUrl() { return url; }
    public String getMd5() { return md5; }
    public String getSha256() { return sha256; }
    public String getZstdUrl() { return zstdUrl; }
}
