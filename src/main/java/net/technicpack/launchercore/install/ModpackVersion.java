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

package net.technicpack.launchercore.install;

import com.google.gson.JsonSyntaxException;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ModpackVersion {
    private String version;
    private boolean legacy;

    private ModpackVersion() {
        // Empty constructor for GSON
    }

    public ModpackVersion(String version, boolean legacy) {
        this.version = version;
        this.legacy = legacy;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String getVersion() {
        return version;
    }

    public static ModpackVersion load(File versionFile) {
        if (!versionFile.exists()) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to load version from %s because it does not exist.", versionFile));
            return null;
        }

        try {
            String json = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            return Utils.getGson().fromJson(json, ModpackVersion.class);
        } catch (JsonSyntaxException | IOException e) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to load version from %s", versionFile), e);
            return null;
        }
    }

    public void save(File versionFile) {
        String json = Utils.getGson().toJson(this);

        try {
            FileUtils.writeStringToFile(versionFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to save installed %s", versionFile), e);
        }
    }

    @Override
    public String toString() {
        return String.format("ModpackVersion{version='%s', legacy=%s}", version, legacy);
    }
}
