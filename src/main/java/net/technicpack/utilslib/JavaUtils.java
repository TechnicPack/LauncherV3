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

public class JavaUtils {
    /**
     * Determine if the current OS is 64-bit.
     * If we're in a 32-bit Java runtime, then this will always return false.
     */
    public static boolean is64Bit() {
        String architecture = System.getProperty("os.arch");
        //     x64 on old Java;                 x64 on new Java;                ARM64
        return architecture.equals("x86_64") || architecture.equals("amd64") || architecture.equals("aarch64");
    }

    /**
     * Determine the bitness of the current OS (64 or 32).
     * If we're in a 32-bit Java runtime, this will always return 32.
     */
    public static String getJavaBitness() {
        return is64Bit() ? "64" : "32";
    }

    public static boolean isArm64() {
        String architecture = System.getProperty("os.arch");

        return architecture.equals("aarch64");
    }
}
