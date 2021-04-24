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

package net.technicpack.launchercore.auth;

import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.ResponseException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator;
import net.technicpack.minecraftcore.mojang.auth.MojangAuthenticator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UserModel {
    private IUserType mCurrentUser = null;
    private List<IAuthListener> mAuthListeners = new LinkedList<>();
    private IUserStore mUserStore;
    private MojangAuthenticator mojangAuthenticator;
    private MicrosoftAuthenticator microsoftAuthenticator;

    public UserModel(IUserStore userStore, MojangAuthenticator mojangAuthenticator, MicrosoftAuthenticator microsoftAuthenticator) {
        this.mCurrentUser = null;
        this.mUserStore = userStore;
        this.mojangAuthenticator = mojangAuthenticator;
        this.microsoftAuthenticator = microsoftAuthenticator;
    }

    public IUserType getCurrentUser() {
        return this.mCurrentUser;
    }

    public void setCurrentUser(IUserType user) {
        this.mCurrentUser = user;

        if (user != null)
            setLastUser(user);
        this.triggerAuthListeners();
    }

    public void addAuthListener(IAuthListener listener) {
        this.mAuthListeners.add(listener);
    }

    protected void triggerAuthListeners() {
        for (IAuthListener listener : mAuthListeners) {
            listener.userChanged(this.mCurrentUser);
        }
    }

    public void startupAuth() {
        IUserType user = getLastUser();

        if (user == null) {
            setCurrentUser(null);
            return;
        }

        try {
            //TODO: Double check this init login
            user.login(this);
            setCurrentUser(user);
        } catch (SessionException | ResponseException ex) {
            setCurrentUser(null);
        } catch (AuthenticationException ex) {
            //TODO: This should handle offline users for Microsoft accounts
            setCurrentUser(mojangAuthenticator.createOfflineUser(user.getDisplayName()));
        }
    }

    public Collection<IUserType> getUsers() {
        return mUserStore.getSavedUsers();
    }

    public IUserType getLastUser() {
        return mUserStore.getUser(mUserStore.getLastUser());
    }

    public IUserType getUser(String username) {
        return mUserStore.getUser(username);
    }

    public void addUser(IUserType user) {
        mUserStore.addUser(user);
    }

    public void removeUser(IUserType user) {
        mUserStore.removeUser(user.getUsername());
    }

    public void setLastUser(IUserType user) {
        mUserStore.setLastUser(user.getUsername());
    }

    public MicrosoftAuthenticator getMicrosoftAuthenticator() {
        return microsoftAuthenticator;
    }

    public MojangAuthenticator getMojangAuthenticator() {
        return mojangAuthenticator;
    }
}
