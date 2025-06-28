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

package net.technicpack.launchercore.launch.java.version;

import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.utilslib.OperatingSystem;

import java.io.File;
import java.util.Objects;

/**
 * An IJavaRuntime for the version of java that is currently running this code.
 */
public final class CurrentJavaRuntime implements IJavaRuntime {
    private final String version;
    private final String vendor;
    private final boolean is64Bit;
    private final String osArch;
    private final File executableFile;

    public CurrentJavaRuntime() {
        this.version = System.getProperty("java.version");
        this.vendor = System.getProperty("java.vendor");
        this.osArch = System.getProperty("os.arch");
        this.is64Bit = this.osArch.contains("64");
        this.executableFile = new File(OperatingSystem.getJavaDir()).getAbsoluteFile();

    }

    @Override
    public File getExecutableFile() {
        return executableFile;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public String getOsArch() {
        return osArch;
    }

    @Override
    public String getBitness() {
        return is64Bit ? "64" : "32";
    }

    @Override
    public boolean is64Bit() {
        return is64Bit;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CurrentJavaRuntime that = (CurrentJavaRuntime) o;
        return is64Bit == that.is64Bit && Objects.equals(version, that.version) && Objects.equals(vendor, that.vendor) && Objects.equals(osArch, that.osArch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, vendor, is64Bit, osArch);
    }

    @Override
    public String toString() {
        return "CurrentJavaRuntime{" +
                "version='" + version + '\'' +
                ", vendor='" + vendor + '\'' +
                ", is64Bit=" + is64Bit +
                ", osArch='" + osArch + '\'' +
                ", executableFile=" + executableFile +
                '}';
    }
}
