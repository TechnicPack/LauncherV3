package net.technicpack.launchercore.image;

import net.technicpack.launchercore.auth.User;

public interface ISkinStore {
	void downloadUserSkin(User user, String location);
	void downloadUserFace(User user, String location);
}
