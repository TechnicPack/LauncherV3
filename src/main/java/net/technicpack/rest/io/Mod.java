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

package net.technicpack.rest.io;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"unused"})
public class Mod extends Resource {
    private String name;
    private String version;

    public Mod() {

    }

    public Mod(String name, String version, String url, String md5) {
        super(url, md5);
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public File generateSafeCacheFile(File cacheDir) throws IOException {
        String filename;
        if (version != null && !version.isEmpty()) {
            filename = name + "-" + version + ".zip";
        } else {
            filename = name + ".zip";
        }

        // Sanitize filename by replacing invalid characters
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "-");

        File filePath = new File(cacheDir, filename);

        if (!filePath.toPath().normalize().startsWith(cacheDir.toPath())) {
            throw new IOException("Unsafe mod cache path detected (" + filePath + ") with base " + cacheDir);
        }

        return filePath;
    }

    @Override
    public String toString() {
        return "Mod{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", url='" + getUrl() + '\'' +
                ", md5='" + getMd5() + '\'' +
                '}';
    }
}
