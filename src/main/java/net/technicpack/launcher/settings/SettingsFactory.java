/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.settings;

import com.google.gson.JsonSyntaxException;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class SettingsFactory {
    public static TechnicSettings buildSettingsObject() {
        File portableSettingsDir = getPortableSettingsDir();

        TechnicSettings portableSettings = tryGetSettings(portableSettingsDir);

        if (portableSettings != null && portableSettings.isPortable())
            return portableSettings;

        File installedSettingsDir = getTechnicHomeDir();

        TechnicSettings settings = tryGetSettings(installedSettingsDir);

        return settings;
    }

    private static TechnicSettings tryGetSettings(File rootDir) {
        if (!rootDir.exists())
            return null;

        File settingsFile = new File(rootDir, "settings.json");
        if (settingsFile == null || !settingsFile.exists())
            return null;

        try {
            String json = FileUtils.readFileToString(settingsFile, Charset.forName("UTF-8"));
            TechnicSettings settings = Utils.getGson().fromJson(json, TechnicSettings.class);

            if (settings != null)
                settings.setFilePath(settingsFile);

            return settings;
        } catch (JsonSyntaxException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load version from " + settingsFile);
            return null;
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load version from " + settingsFile);
            return null;
        }
    }

    private static File getPortableSettingsDir() {
        return new File("."+File.separator+"technic");
    }

    public static File getTechnicHomeDir() {
        String userHome = System.getProperty("user.home", ".");

        OperatingSystem os = OperatingSystem.getOperatingSystem();
        switch (os) {
            case LINUX:
                return new File(userHome, ".technic/");
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    return new File(applicationData, ".technic/");
                } else {
                    return new File(userHome, ".technic/");
                }
            case OSX:
                return new File(userHome, "Library/Application Support/technic");
            case UNKNOWN:
                return new File(userHome, "technic/");
            default:
                return new File(userHome, "technic/");
        }
    }
}
