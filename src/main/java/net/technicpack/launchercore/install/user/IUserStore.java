package net.technicpack.launchercore.install.user;

import java.util.Collection;

public interface IUserStore {
    void addUser(User user);

    void removeUser(String username);

    User getUser(String username);

    String getClientToken();

    Collection<String> getUsers();

    Collection<User> getSavedUsers();

    void setLastUser(String username);

    String getLastUser();
}
