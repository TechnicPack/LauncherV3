/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2020 Syndicate, LLC
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

package net.technicpack.utilslib;

import net.technicpack.launchercore.JavaVersionComparator;

public class JavaUtils {
    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String JAVA_BITNESS = OS_ARCH.contains("64") ? "64" : "32";
    private static final JavaVersionComparator JAVA_VERSION_COMPARATOR = new JavaVersionComparator();


    private JavaUtils() {
        // Prevent initialization of this utility class
        throw new IllegalStateException("Utility class");
    }

    public static boolean isArm64() {
        return OS_ARCH.equals("aarch64");
    }

    /**
     * Compares two Java version strings.
     * @return < 0 if v1 < v2, 0 if v1 == v2, > 0 if v1 > v2
     * @see JavaVersionComparator#compare(String, String)
     */
    public static int compareVersions(String v1, String v2) {
        return JAVA_VERSION_COMPARATOR.compare(v1, v2);
    }
}
