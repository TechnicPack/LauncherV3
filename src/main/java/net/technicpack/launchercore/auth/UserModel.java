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

import net.technicpack.launcher.io.UserStore;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.exception.ResponseException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import net.technicpack.utilslib.Utils;

import javax.swing.JOptionPane;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class UserModel {
    private IUserType mCurrentUser;
    private List<IAuthListener> mAuthListeners = new LinkedList<>();
    private UserStore mUserStore;
    private MicrosoftAuthenticator microsoftAuthenticator;

    public UserModel(UserStore userStore, MicrosoftAuthenticator microsoftAuthenticator) {
        this.mCurrentUser = null;
        this.mUserStore = userStore;
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
            user.login(this);
            addUser(user);
            setCurrentUser(user);
        } catch (SessionException | ResponseException e) {
            setCurrentUser(null);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
        } catch (AuthenticationException e) {
            Utils.getLogger().log(Level.SEVERE, "Authentication error, running in offline mode", e);
            JOptionPane.showMessageDialog(null, "Due to an authentication error, you're playing in offline mode.\n\nUntil you are properly logged in you won't be able to connect to multiplayer servers.", "Offline mode", JOptionPane.WARNING_MESSAGE);
            // Create offline mode user
            setCurrentUser(new MicrosoftUser(user.getId(), user.getUsername()));
        }
    }

    public Collection<IUserType> getUsers() {
        return mUserStore.getSavedUsers();
    }

    public IUserType getLastUser() {
        return mUserStore.getUser(mUserStore.getLastUser());
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

}
