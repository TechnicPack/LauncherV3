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
import net.technicpack.minecraftcore.live.auth.LiveLoginBrowser;
import net.technicpack.minecraftcore.live.auth.LiveUser;
import net.technicpack.minecraftcore.live.auth.OnLoggedInListener;
import net.technicpack.minecraftcore.live.auth.XboxAuthenticationService;
import net.technicpack.minecraftcore.live.auth.response.MinecraftProfile;
import net.technicpack.minecraftcore.mojang.auth.MojangAuthenticationService;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UserModel {
    private IUserType mCurrentUser = null;
    private List<IAuthListener> mAuthListeners = new LinkedList<IAuthListener>();
    private IUserStore mUserStore;
    private MojangAuthenticationService mojangAuthService;
    private XboxAuthenticationService liveAuthService;

    public UserModel(IUserStore userStore, MojangAuthenticationService mojangAuthService, XboxAuthenticationService liveAuthService) {
        this.mCurrentUser = null;
        this.mUserStore = userStore;
        this.mojangAuthService = mojangAuthService;
        this.liveAuthService = liveAuthService;
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

    public AuthError attemptMojangUserRefresh(MojangUser user) throws AuthenticationNetworkFailureException {
        IAuthResponse response = mojangAuthService.requestRefresh(user);
        if (response == null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError("Session Error", "Please log in again.");
        } else if (response.getError() != null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError(response.getError(), response.getErrorMessage());
        } else {
            //Refresh user from response
            user = mojangAuthService.createClearedUser(user.getUsername(), response);
            mUserStore.addUser(user);
            setCurrentUser(user);
            return null;
        }
    }

    public AuthError attemptLiveUserRefresh(LiveUser user) {
        liveAuthService.LiveRefreshToken(user.getRefreshToken());
        MinecraftProfile profile = liveAuthService.GetMinecraftProfile();
        user.setRefreshToken(liveAuthService.GetLiveRefreshToken());
        setCurrentUser(user);
        return null;
    }

    public AuthError attemptMojangInitialLogin(String username, String password) {
        try {
            IAuthResponse response = mojangAuthService.requestLogin(username, password, getClientToken());

            if (response == null) {
                return new AuthError("Auth Error", "Invalid credentials. Invalid username or password.");
            } else if (response.getError() != null) {
                return new AuthError(response.getError(), response.getErrorMessage());
            } else {
                //Create an online user with the received data
                IUserType clearedUser = mojangAuthService.createClearedUser(username, response);
                setCurrentUser(clearedUser);
                return null;
            }
        } catch (AuthenticationNetworkFailureException ex) {
            ex.printStackTrace();
            return new AuthError("Auth Servers Inaccessible", "An error occurred while attempting to reach " + ex.getTargetSite());
        }
    }

    public void attemptLiveInitialLogin(OnLiveLoggedInListener listener) {
        String url = liveAuthService.GetOauthUrl();
        LiveLoginBrowser browser = new LiveLoginBrowser(url, new OnLoggedInListener() {
            @Override
            public void onLoggedIn(Map<String, String> params) {
                liveAuthService.LiveAuthToken(params.get("code"));
                MinecraftProfile profile = liveAuthService.GetMinecraftProfile();

                IUserType clearedUser = new LiveUser(profile, liveAuthService.GetMinecraftAccessToken(), liveAuthService.GetLiveRefreshToken());
                setCurrentUser(clearedUser);

                listener.onLoggedIn();
            }
        });
        browser.setVisible(true);
    }

    public void initAuth() {
        IUserType user = getLastUser();

        if (user instanceof MojangUser) {
            try {
                AuthError error = this.attemptMojangUserRefresh((MojangUser) user);

                if (error != null)
                    setCurrentUser(null);
            } catch (AuthenticationNetworkFailureException ex) {
                setCurrentUser(mojangAuthService.createOfflineUser(user.getDisplayName()));
            }
        } else if (user instanceof LiveUser) {
            attemptLiveUserRefresh((LiveUser) user);
        } else
            setCurrentUser(null);
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
