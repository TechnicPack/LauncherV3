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
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class TechnicUserStore implements IUserStore {
    private String clientToken = UUID.randomUUID().toString();
    private Map<String, IUserType> savedUsers = new HashMap<>();
    private String lastUser;
    private transient File usersFile;

    public TechnicUserStore() {
    }

    public TechnicUserStore(File userFile) {
        this.usersFile = userFile;
    }

    public static TechnicUserStore load(File userFile) {
        if (!userFile.exists()) {
            Utils.getLogger().log(Level.WARNING, "Unable to load users from " + userFile + " because it does not exist.");
            return new TechnicUserStore(userFile);
        }

        try {
            String json = FileUtils.readFileToString(userFile, StandardCharsets.UTF_8);
            TechnicUserStore newModel = MojangUtils.getGson().fromJson(json, TechnicUserStore.class);

            if (newModel != null) {
                newModel.setUserFile(userFile);
                return newModel;
            }
        } catch (JsonSyntaxException | IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to load users from " + userFile);
        }

        return new TechnicUserStore(userFile);
    }

    public void setUserFile(File userFile) {
        this.usersFile = userFile;
    }

    public void save() {
        String json = MojangUtils.getGson().toJson(this);

        try {
            FileUtils.writeStringToFile(usersFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to save users " + usersFile);
        }
    }

    public void addUser(IUserType user) {
        if (savedUsers.containsKey(user.getUsername())) {
            IUserType oldUser = savedUsers.get(user.getUsername());
            if (oldUser instanceof MojangUser && user instanceof MojangUser) {
                ((MojangUser) user).mergeUserProperties((MojangUser) oldUser);
            }
        }
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

    public String getClientToken() {
        return clientToken;
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
}
