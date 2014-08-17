package net.technicpack.launchercore.auth;

public interface IAuthListener<UserType> {
	void userChanged(UserType user);
}
