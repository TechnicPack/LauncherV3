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
import net.technicpack.utilslib.ProfilingUtils;
import net.technicpack.utilslib.Utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * An IJavaRuntime based on an externally-selected java executable.
 */
public final class FileBasedJavaRuntime implements IJavaRuntime, Serializable {
    private transient boolean queried = false;
    private transient String version;
    private transient String vendor;
    private transient boolean is64Bit;
    private transient File javaPath;
    private String filePath;

    public FileBasedJavaRuntime(File javaPath) {
        // Resolve to absolute paths
        this.filePath = Objects.requireNonNull(javaPath).getAbsolutePath();
        this.javaPath = new File(this.filePath);
    }

    public FileBasedJavaRuntime(String path) {
        Path absolutePath = Paths.get(Objects.requireNonNull(path)).toAbsolutePath();

        this.javaPath = absolutePath.toFile();
        this.filePath = absolutePath.toString();
    }

    public File getExecutableFile() {
        return javaPath;
    }

    @Override
    public String getVersion() {
        ensureQueried();

        return version;
    }

    private void ensureQueried() {
        if (!queried) {
            queried = true;
            ProfilingUtils.measureTime("Querying Java runtime \"" + filePath + "\"",
                    this::getInformationFromJavaRuntime);
        }
    }

    /**
     * Get the version and vendor of the Java executable in javaPath, by executing and querying it
     */
    private void getInformationFromJavaRuntime() {
        Path javaBinaryPath = Paths.get(filePath);

        if (javaBinaryPath.endsWith("javaw.exe")) {
            // On Windows operating systems, we ask people to find javaw.exe, which we use to actually run anything.
            // However, javaw.exe doesn't output anything since it's designed to be quiet, only java.exe does, so
            // we have to change the path to point at java.exe
            javaBinaryPath = javaBinaryPath.resolveSibling("java.exe");
        }

        String data = Utils.getProcessOutput(javaBinaryPath.toString(), "-XshowSettings:properties", "-version");

        if (data == null) return;

        String osArch = null;

        try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("java.version =")) {
                    version = line.substring("java.version =".length()).trim();
                } else if (line.startsWith("java.vendor =")) {
                    vendor = line.substring("java.vendor =".length()).trim();
                } else if (line.startsWith("os.arch =")) {
                    osArch = line.substring("os.arch =".length()).trim();
                }
            }
        } catch (IOException ex) {
            // ignore, it should never happen
        }

        is64Bit = osArch != null && osArch.contains("64");
    }

    @Override
    public String getVendor() {
        ensureQueried();

        return vendor;
    }

    public boolean is64Bit() {
        ensureQueried();

        return is64Bit;
    }

    /**
     * @return True if the javaPath points to a valid version of java that can be run, false otherwise
     */
    public boolean isValid() {
        ensureQueried();

        return (version != null && vendor != null);
    }

    public String getExecutablePath() {
        return filePath;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filePath);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileBasedJavaRuntime that = (FileBasedJavaRuntime) o;
        return Objects.equals(filePath, that.filePath);
    }
}
