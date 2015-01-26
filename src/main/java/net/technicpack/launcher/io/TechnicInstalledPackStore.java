/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.launcher.io;

import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TechnicInstalledPackStore implements IInstalledPackRepository {

    private transient File loadedFile;

    private final Map<String, InstalledPack> installedPacks = new HashMap<String, InstalledPack>();
    private final List<String> byIndex = new ArrayList<String>();
    private String selected = null;

    public TechnicInstalledPackStore(File jsonFile) {
        setLoadedFile(jsonFile);
    }

    public static TechnicInstalledPackStore load(File jsonFile) {
        if (!jsonFile.exists()) {
            Utils.getLogger().log(Level.WARNING, "Unable to load installedPacks from " + jsonFile + " because it does not exist.");
            return new TechnicInstalledPackStore(jsonFile);
        }

        try {
            String json = FileUtils.readFileToString(jsonFile, Charset.forName("UTF-8"));
            TechnicInstalledPackStore parsedList = Utils.getGson().fromJson(json, TechnicInstalledPackStore.class);

            if (parsedList != null) {
                parsedList.setLoadedFile(jsonFile);
                return parsedList;
            } else
                return new TechnicInstalledPackStore(jsonFile);
        } catch (JsonSyntaxException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load installedPacks from " + jsonFile);
            return new TechnicInstalledPackStore(jsonFile);
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load installedPacks from " + jsonFile);
            return new TechnicInstalledPackStore(jsonFile);
        }
    }

    protected void setLoadedFile(File loadedFile) {
        this.loadedFile = loadedFile;

        //HACK: "And that's why.... you don't put view data in the model."
        /////////// - J. Walter Weatherman, Software Developer
        if (installedPacks.containsKey("addpack"))
            installedPacks.remove("addpack");

        if (byIndex.contains("addpack"))
            byIndex.remove("addpack");
    }

    @Override
    public Map<String, InstalledPack> getInstalledPacks() {
        return installedPacks;
    }

    @Override
    public List<String> getPackNames() {
        return byIndex;
    }

    @Override
    public String getSelectedSlug() {
        return selected;
    }

    @Override
    public void setSelectedSlug(String slug) {
        selected = slug;
    }

    @Override
    public InstalledPack put(InstalledPack installedPack) {
        InstalledPack pack = installedPacks.put(installedPack.getName(), installedPack);
        if (pack == null) {
            byIndex.add(installedPack.getName());
        }
        save();
        return pack;
    }

    @Override
    public InstalledPack remove(String name) {
        InstalledPack pack = installedPacks.remove(name);
        if (pack != null) {
            byIndex.remove(name);
        }
        save();
        return pack;
    }

    @Override
    public void save() {
        String json = Utils.getGson().toJson(this);

        try {
            FileUtils.writeStringToFile(loadedFile, json, Charset.forName("UTF-8"));
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to save settings " + loadedFile, e);
        }
    }
}
