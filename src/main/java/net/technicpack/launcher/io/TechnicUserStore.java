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
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.utilslib.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TechnicUserStore implements IUserStore, Serializable {
    private Map<String, IUserType> savedUsers = new HashMap<>();
    private String lastUser;
    private transient File usersFile;

    @SuppressWarnings("unused")
    private TechnicUserStore() {
        // Empty constructor for GSON
    }

    protected TechnicUserStore(File userFile) {
        this.usersFile = userFile;
    }

    public static TechnicUserStore load(File userFile) {
        if (!userFile.exists()) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to load users from %s because it does not exist.", userFile));
            return new TechnicUserStore(userFile);
        }

        try {
            TechnicUserStore newModel;

            try (Reader reader = Files.newBufferedReader(userFile.toPath(), StandardCharsets.UTF_8)) {
                newModel = MojangUtils.getGson().fromJson(reader, TechnicUserStore.class);
            }

            if (newModel != null) {
                newModel.cleanupSavedUsers();
                newModel.setUserFile(userFile);
                return newModel;
            }
        } catch (JsonParseException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Unable to load users from %s", userFile), e);
        }

        return new TechnicUserStore(userFile);
    }

    public void setUserFile(File userFile) {
        this.usersFile = userFile;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(usersFile.toPath(), StandardCharsets.UTF_8)) {
            MojangUtils.getGson().toJson(this, writer);
        } catch (JsonIOException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Unable to save users file %s", usersFile), e);
        }
    }

    public void addUser(IUserType user) {
        savedUsers.put(user.getUsername(), user);
        save();
    }

    public void removeUser(String username) {
        savedUsers.remove(username);
        save();
    }

    public IUserType getUser(String accountName) {
        return savedUsers.get(accountName);
    }

    public Collection<String> getUsers() {
        return savedUsers.keySet();
    }

    public Collection<IUserType> getSavedUsers() {
        return savedUsers.values();
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
        save();
    }

    public String getLastUser() {
        return lastUser;
    }

    private void cleanupSavedUsers() {
        savedUsers.values().removeAll(Collections.singleton(null));
    }
}
