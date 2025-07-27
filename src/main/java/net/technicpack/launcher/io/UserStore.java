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
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.utilslib.Utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class UserStore {
    private Map<String, IUserType> savedUsers = new HashMap<>();
    private String lastUser;

    @SuppressWarnings("java:S2065")
    private transient Path storePath;

    @SuppressWarnings("unused")
    private UserStore() {
        // Empty constructor for GSON
    }

    private UserStore(Path storePath) {
        this.storePath = storePath.toAbsolutePath();
    }

    public static UserStore load(Path storePath) {
        if (!Files.exists(storePath)) {
            Utils.getLogger().log(Level.WARNING, String.format("Unable to load users from %s because it does not exist", storePath));
            return new UserStore(storePath);
        }

        try {
            UserStore newModel;

            try (Reader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
                newModel = MojangUtils.getGson().fromJson(reader, UserStore.class);
            }

            // This is required because Gson doesn't allow post-processing after deserialization
            if (newModel != null) {
                newModel.removeNullUsers();
                newModel.setStorePath(storePath);
                return newModel;
            }
        } catch (JsonParseException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Failed to load users from %s", storePath), e);
        }

        return new UserStore(storePath);
    }

    private void setStorePath(Path storePath) {
        this.storePath = storePath;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(storePath, StandardCharsets.UTF_8)) {
            MojangUtils.getGson().toJson(this, writer);
        } catch (JsonIOException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Failed to save users to %s", storePath), e);
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

    private void removeNullUsers() {
        savedUsers.values().removeAll(Collections.singleton(null));
    }
}
