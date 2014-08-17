package net.technicpack.launchercore.auth;

import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;

public interface IGameAuthService<UserData> {
    public UserData createClearedUser(String username, IAuthResponse response);
    public UserData createOfflineUser(String displayName);
    public IAuthResponse requestRefresh(UserData user) throws AuthenticationNetworkFailureException;
    public IAuthResponse requestLogin(String username, String password, String data) throws AuthenticationNetworkFailureException;
}
