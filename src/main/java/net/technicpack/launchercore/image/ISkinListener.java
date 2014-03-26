package net.technicpack.launchercore.image;

import net.technicpack.launchercore.auth.User;

public interface ISkinListener {
	void skinReady(User user);
	void faceReady(User user);
}
