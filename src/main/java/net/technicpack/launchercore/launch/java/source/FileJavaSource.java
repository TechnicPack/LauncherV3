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
import net.technicpack.launchercore.launch.java.version.FileBasedJavaVersion;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads versions from an external file
 */
public class FileJavaSource implements IVersionSource {
    private transient File loadedFile;
    private List<FileBasedJavaVersion> versions = new ArrayList<FileBasedJavaVersion>();

    protected FileJavaSource(File loadFile) {
        this.loadedFile = loadFile;
    }

    protected void setLoadedFile(File loadedFile) { this.loadedFile = loadedFile; }

    @Override
    public void enumerateVersions(JavaVersionRepository repository) {
        for(FileBasedJavaVersion version : versions) {
            repository.addVersion(version);
        }
    }

    public void addVersion(FileBasedJavaVersion version) {
        versions.add(version);
        save();
    }

    public static FileJavaSource load(File file) {

        if (file == null || !file.exists())
            return new FileJavaSource(file);

        try {
            String sourceText = FileUtils.readFileToString(file);
            FileJavaSource source = Utils.getGson().fromJson(sourceText, FileJavaSource.class);
            source.setLoadedFile(file);
            return source;
        } catch (IOException ex) {
            ex.printStackTrace();
            return new FileJavaSource(file);
        }
    }

    public void save() {
        String data = Utils.getGson().toJson(this);

        try {
            FileUtils.write(loadedFile, data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
