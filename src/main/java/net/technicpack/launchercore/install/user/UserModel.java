package net.technicpack.launchercore.install.user;

import net.technicpack.launchercore.auth.AuthResponse;
import net.technicpack.launchercore.auth.AuthenticationService;
import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.mirror.secure.rest.ISecureMirror;
import net.technicpack.launchercore.mirror.secure.rest.ValidateRequest;
import net.technicpack.launchercore.mirror.secure.rest.ValidateResponse;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UserModel {
    private static UserModel mInstance = null;

    private User mCurrentUser = null;
    private List<IAuthListener> mAuthListeners = new LinkedList<IAuthListener>();
    private IUserStore mUserStore;

    public UserModel(IUserStore userStore) {
        this.mCurrentUser = null;
        this.mUserStore = userStore;
    }

    public User getCurrentUser() {
        return this.mCurrentUser;
    }

    public void setCurrentUser(User user) {
        this.mCurrentUser = user;
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

    public AuthError AttemptLastUserRefresh() throws AuthenticationNetworkFailureException {
        String lastUser = mUserStore.getLastUser();

        if (lastUser == null || lastUser.isEmpty()) {
            return new AuthError("No cached user", "Could not log into the last logged in user, as there was no cached user to log into.");
        }

        User user = mUserStore.getUser(lastUser);

        if (user == null) {
            return new AuthError("No cached user", "Could not log into the specified user, as there was no cached user to log into.");
        }

        return AttemptUserRefresh(user);
    }

    public AuthError AttemptUserRefresh(User user) throws AuthenticationNetworkFailureException {
        AuthResponse response = AuthenticationService.requestRefresh(user);
        if (response == null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError("Session Error", "Please log in again.");
        } else if (response.getError() != null) {
            mUserStore.removeUser(user.getUsername());
            return new AuthError(response.getError(), response.getErrorMessage());
        } else {
            //Refresh user from response
            user = new User(user.getUsername(), response);
            mUserStore.addUser(user);
            setCurrentUser(user);
            return null;
        }
    }

    public String retrieveDownloadToken(ISecureMirror mirror) throws DownloadException {
        if (this.getCurrentUser() == null)
            return null;

        ValidateResponse response = mirror.validate(new ValidateRequest(this.getCurrentUser().getClientToken(), this.getCurrentUser().getAccessToken()));

        if (!response.wasValid())
            throw new DownloadException(response.getErrorMessage());

        //this.getCurrentUser().rotateAccessToken(response.getAccessToken());
        //mUserStore.addUser(this.getCurrentUser());
        return response.getDownloadToken();
    }

    public Collection<User> getUsers() {
        return mUserStore.getSavedUsers();
    }

    public User getLastUser() {
        return mUserStore.getUser(mUserStore.getLastUser());
    }

    public User getUser(String username) {
        return mUserStore.getUser(username);
    }

    public void addUser(User user) {
        mUserStore.addUser(user);
    }

    public void removeUser(User user) {
        mUserStore.removeUser(user.getUsername());
    }

    public void setLastUser(User user) {
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
