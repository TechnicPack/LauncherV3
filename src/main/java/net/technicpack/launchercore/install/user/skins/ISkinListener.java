package net.technicpack.launchercore.install.user.skins;

import net.technicpack.launchercore.install.user.User;

public interface ISkinListener {
    void skinReady(User user);

    void faceReady(User user);
}
