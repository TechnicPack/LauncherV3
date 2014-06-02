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

package net.technicpack.launchercore.install;

import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class Version {
    private String version;
    private boolean legacy;

    public Version() {

    }

    public Version(String version, boolean legacy) {
        this.version = version;
        this.legacy = legacy;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public static Version load(File version) {
        if (!version.exists()) {
            Utils.getLogger().log(Level.WARNING, "Unable to load version from " + version + " because it does not exist.");
            return null;
        }

        try {
            String json = FileUtils.readFileToString(version, Charset.forName("UTF-8"));
            return Utils.getGson().fromJson(json, Version.class);
        } catch (JsonSyntaxException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load version from " + version);
            return null;
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load version from " + version);
            return null;
        }
    }

    public void save(File saveDirectory) {
        File version = new File(saveDirectory, "version");
        String json = Utils.getGson().toJson(this);

        try {
            FileUtils.writeStringToFile(version, json, Charset.forName("UTF-8"));
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to save installed " + version);
        }
    }

    @Override
    public String toString() {
        return version + " " + legacy;
    }
}
