/**
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

package net.technicpack.launchercore;

import net.technicpack.autoupdate.IBuildNumber;

public class TechnicConstants {
    public static final String technicURL = "https://mirror.technicpack.net/Technic/";
    public static final String technicVersions = technicURL + "version/";
    public static final String technicFmlLibRepo = technicURL + "lib/fml/";
    public static final String technicLibRepo = technicURL + "lib/";
    public static final String javaDownloadUrl = "https://www.java.com/en/download/manual.jsp";

    private static IBuildNumber buildNumber;
    private static String userAgent;

    public static IBuildNumber getBuildNumber() {
        return buildNumber;
    }

    public static void setBuildNumber(IBuildNumber buildNumber) {
        TechnicConstants.buildNumber = buildNumber;

        userAgent = "Mozilla/5.0 (Java) TechnicLauncher/4." + buildNumber.getBuildNumber();
    }

    public static String getUserAgent() {
        return userAgent;
    }
}
