package net.technicpack.launchercore.install.user.skins;

import net.technicpack.launchercore.install.user.User;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.util.Utils;

import java.io.IOException;
import java.util.logging.Level;

public class MinotarSkinStore implements ISkinStore {
    private String mBaseUrl;
    private MirrorStore mirrorStore;

    public MinotarSkinStore(String baseUrl, MirrorStore mirrorStore) {
        mBaseUrl = baseUrl;
        this.mirrorStore = mirrorStore;
    }

    @Override
    public void downloadUserSkin(User user, String location) {
        try {
            mirrorStore.downloadFile(mBaseUrl + "skin/" + user.getDisplayName(), user.getDisplayName(), location);
        } catch (IOException e) {
            Utils.getLogger().log(Level.INFO, "Error downloading user face image: " + user.getDisplayName(), e);
        }
    }

    @Override
    public void downloadUserFace(User user, String location) {
        try {
            mirrorStore.downloadFile(mBaseUrl + "helm/" + user.getDisplayName() + "/100", user.getDisplayName(), location);
        } catch (IOException e) {
            Utils.getLogger().log(Level.INFO, "Error downloading user face image: " + user.getDisplayName(), e);
        }
    }
}
