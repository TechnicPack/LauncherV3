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

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TechnicInstalledPackStore implements IInstalledPackRepository {
    @SuppressWarnings("java:S2065")
    private transient Path storePath;

    private final Map<String, InstalledPack> installedPacks = new HashMap<>();
    private final List<String> byIndex = new ArrayList<>();
    private String selected = null;

    public TechnicInstalledPackStore(Path storePath) {
        setStorePath(storePath);
    }

    @SuppressWarnings("unused")
    private TechnicInstalledPackStore() {
        // Empty constructor for GSON
    }

    public static TechnicInstalledPackStore load(File jsonFile) {
        Path storePath = jsonFile.toPath().toAbsolutePath();

        if (!Files.exists(storePath)) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to load installedPacks from %s because it does not exist", storePath));
            return new TechnicInstalledPackStore(storePath);
        }

        try {
            try (Reader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
                TechnicInstalledPackStore parsedList = Utils.getGson().fromJson(reader, TechnicInstalledPackStore.class);

                if (parsedList == null) {
                    return new TechnicInstalledPackStore(storePath);
                }

                parsedList.setStorePath(storePath);
                return parsedList;
            }
        } catch (JsonParseException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Failed to load installedPacks from %s", storePath), e);
            return new TechnicInstalledPackStore(storePath);
        }
    }

    protected void cleanUpLegacyEntries() {
        //HACK: "And that's why.... you don't put view data in the model."
        /////////// - J. Walter Weatherman, Software Developer
        installedPacks.remove("addpack");
        byIndex.remove("addpack");
    }

    protected void setStorePath(Path storePath) {
        this.storePath = storePath;

        cleanUpLegacyEntries();
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
        save();
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

    // TODO: all of this should be syncronized on the file
    @Override
    public void save() {
        // First we write to a temp file, then we move that file to the intended path.
        // This way, we won't end up with an empty file if we fail to write to it.

        Path tmp = storePath.resolveSibling(storePath.getFileName() + ".tmp");

        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            Utils.getGson().toJson(this, writer);

            Files.move(tmp, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | JsonIOException e) {
            // Clean up the temp file
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // We can safely continue, even if the temp wasn't deleted
            }

            Utils.getLogger().log(Level.SEVERE, String.format("Failed to save installedPacks to %s", storePath), e);
        }
    }
}
