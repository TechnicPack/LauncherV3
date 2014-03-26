package net.technicpack.launchercore.image;

import net.technicpack.launchercore.auth.User;

import java.awt.image.BufferedImage;

public interface ISkinMapper {
	String getSkinFilename(User user);
	String getFaceFilename(User user);
	BufferedImage getDefaultSkinImage();
	BufferedImage getDefaultFaceImage();
}
