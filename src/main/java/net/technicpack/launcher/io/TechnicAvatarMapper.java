package net.technicpack.launcher.io;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.image.IImageMapper;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.FeedItem;

import java.awt.image.BufferedImage;
import java.io.File;

public class TechnicAvatarMapper implements IImageMapper<AuthorshipInfo> {
    private LauncherDirectories directories;
    private BufferedImage defaultImage;

    public TechnicAvatarMapper(LauncherDirectories directories, ResourceLoader resources) {
        this.directories = directories;
        defaultImage = resources.getImage("icon.png");
    }

    @Override
    public boolean shouldDownloadImage(AuthorshipInfo imageKey) {
        return true;
    }

    @Override
    public File getImageLocation(AuthorshipInfo imageKey) {
        return new File(directories.getAssetsDirectory(), "avatars" + File.separator + "gravitar" + File.separator + imageKey.getUser() + ".png");
    }

    @Override
    public BufferedImage getDefaultImage() {
        return defaultImage;
    }
}
