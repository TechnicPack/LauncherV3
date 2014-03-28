package net.technicpack.launchercore.auth;

import net.technicpack.minecraftcore.mojang.auth.AuthResponse;
import net.technicpack.minecraftcore.mojang.auth.AuthenticationService;
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
		for(IAuthListener listener : mAuthListeners) {
			listener.userChanged(this.mCurrentUser);
		}
	}

	public AuthError attemptUserRefresh(User user) throws AuthenticationNetworkFailureException {
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

    public AuthError attemptInitialLogin(String username, String password) {
        try {
            AuthResponse response = AuthenticationService.requestLogin(username, password, getClientToken());

            if (response == null) {
                return new AuthError("Auth Error","Invalid credentials. Invalid username or password.");
            } else if (response.getError() != null) {
                return new AuthError(response.getError(), response.getErrorMessage());
            } else {
                //Create an online user with the received data
                User clearedUser = new User(username, response);
                setCurrentUser(clearedUser);
                return null;
            }
        } catch (AuthenticationNetworkFailureException ex) {
            return new AuthError("Auth Servers Inaccessible", "An error occurred while attempting to reach Minecraft.net");
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

    public void initAuth() {
        User user = getLastUser();

        if (user != null) {
            try {
                AuthError error = this.attemptUserRefresh(user);

                if (error != null)
                    user = null;
            } catch (AuthenticationNetworkFailureException ex) {
                user = new User(user.getDisplayName());
            }
        }

        this.setCurrentUser(user);
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
