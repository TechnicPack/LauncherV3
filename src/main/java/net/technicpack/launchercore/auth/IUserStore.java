package net.technicpack.launchercore.auth;

import java.util.Collection;

public interface IUserStore<UserType> {
	void addUser(UserType user);
	void removeUser(String username);
    UserType getUser(String username);

	String getClientToken();

	Collection<String> getUsers();
	Collection<UserType> getSavedUsers();

	void setLastUser(String username);
	String getLastUser();
}
