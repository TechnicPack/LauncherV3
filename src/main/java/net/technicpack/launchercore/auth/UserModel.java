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

import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UserModel<UserType extends IUserType> {
    private UserType mCurrentUser = null;
    private List<IAuthListener> mAuthListeners = new LinkedList<IAuthListener>();
    private IUserStore<UserType> mUserStore;
    private IGameAuthService<UserType> gameAuthService;

    public UserModel(IUserStore userStore, IGameAuthService<UserType> gameAuthService) {
        this.mCurrentUser = null;
        this.mUserStore = userStore;
        this.gameAuthService = gameAuthService;
    }

    public UserType getCurrentUser() {
        return this.mCurrentUser;
    }

    public void setCurrentUser(UserType user) {
        this.mCurrentUser = user;

        if (user != null)
            setLastUser(user);
        this.triggerAuthListeners();
    }

    public void addAuthListener(IAuthListener<UserType> listener) {
        this.mAuthListeners.add(listener);
    }

    protected void triggerAuthListeners() {
        for (IAuthListener<UserType> listener : mAuthListeners) {
            listener.userChanged(this.mCurrentUser);
        }
    }

    public AuthError attemptUserRefresh(UserType user) throws AuthenticationNetworkFailureException {
        IAuthResponse response = gameAuthService.requestRefresh(user);
        if (response == null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError("Session Error", "Please log in again.");
        } else if (response.getError() != null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError(response.getError(), response.getErrorMessage());
        } else {
            //Refresh user from response
            user = gameAuthService.createClearedUser(user.getUsername(), response);
            mUserStore.addUser(user);
            setCurrentUser(user);
            return null;
        }
    }

    public AuthError attemptInitialLogin(String username, String password) {
        try {
            IAuthResponse response = gameAuthService.requestLogin(username, password, getClientToken());

            if (response == null) {
                return new AuthError("Auth Error", "Invalid credentials. Invalid username or password.");
            } else if (response.getError() != null) {
                return new AuthError(response.getError(), response.getErrorMessage());
            } else {
                //Create an online user with the received data
                UserType clearedUser = gameAuthService.createClearedUser(username, response);
                setCurrentUser(clearedUser);
                return null;
            }
        } catch (AuthenticationNetworkFailureException ex) {
            ex.printStackTrace();
            return new AuthError("Auth Servers Inaccessible", "An error occurred while attempting to reach " + ex.getTargetSite());
        }
    }

    public void initAuth() {
        UserType user = getLastUser();

        if (user != null) {
            try {
                AuthError error = this.attemptUserRefresh(user);

                if (error != null)
                    setCurrentUser(null);
            } catch (AuthenticationNetworkFailureException ex) {
                setCurrentUser(gameAuthService.createOfflineUser(user.getDisplayName()));
            }
        } else
            setCurrentUser(null);
    }

    public Collection<UserType> getUsers() {
        return mUserStore.getSavedUsers();
    }

    public UserType getLastUser() {
        return mUserStore.getUser(mUserStore.getLastUser());
    }

    public UserType getUser(String username) {
        return mUserStore.getUser(username);
    }

    public void addUser(UserType user) {
        mUserStore.addUser(user);
    }

    public void removeUser(UserType user) {
        mUserStore.removeUser(user.getUsername());
    }

    public void setLastUser(UserType user) {
        mUserStore.setLastUser(user.getUsername());
    }

    public String getClientToken() {
        return mUserStore.getClientToken();
    }

    public class AuthError {
        private String mError;
        private String mErrorDescription;

        public AuthError(String error, String errorDescription) {
            this.mError = error;
            this.mErrorDescription = errorDescription;
        }

        public String getError() {
            return mError;
        }

        public String getErrorDescription() {
            return mErrorDescription;
        }
    }
}
