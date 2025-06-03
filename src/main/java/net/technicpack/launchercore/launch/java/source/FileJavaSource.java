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

package net.technicpack.launchercore.launch.java.source;

import net.technicpack.launchercore.launch.java.IVersionSource;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaRuntime;
import net.technicpack.utilslib.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads versions from an external file
 */
public class FileJavaSource implements IVersionSource, Serializable {
    private transient File loadedFile;
    private List<FileBasedJavaRuntime> versions = new ArrayList<>();

    protected FileJavaSource(File loadFile) {
        this.loadedFile = loadFile;
    }

    protected void setLoadedFile(File loadedFile) { this.loadedFile = loadedFile; }

    @Override
    public void enumerateVersions(JavaVersionRepository repository) {
        // Add all valid Java runtimes to the repository, and remove any invalid ones
        // If any were removed, then we save the cleaned up list
        if (versions.removeIf(version -> !repository.addVersion(version))) {
            save();
        }
    }

    public void addVersion(FileBasedJavaRuntime version) {
        versions.add(version);
        save();
    }

    public static FileJavaSource load(File file) {
        if (file == null || !file.exists()) return new FileJavaSource(file);

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            FileJavaSource source = Utils.getGson().fromJson(reader, FileJavaSource.class);
            source.setLoadedFile(file);
            return source;
        } catch (IOException ex) {
            Utils.getLogger().log(Level.SEVERE, "Failed to load Java versions file", ex);
            return new FileJavaSource(file);
        }
    }

    public void save() {
        try (FileOutputStream fos = new FileOutputStream(loadedFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            Utils.getGson().toJson(this, writer);
        } catch (IOException ex) {
            Utils.getLogger().log(Level.SEVERE, "Failed to save Java versions file", ex);
        }
    }
}
