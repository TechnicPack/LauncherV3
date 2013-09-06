package net.technicpack.launcher;

import net.technicpack.launcher.util.DownloadUtils;

import java.io.IOException;

public class TechnicLauncher {

	public static void main(String[] args) {
		try {
			DownloadUtils.downloadFile("http://mirror.technicpack.net/Technic/tekkit/resources/logo_180.png", "E:/logo.png");
			DownloadUtils.downloadFile("https://s3.amazonaws.com/Minecraft.Download/versions/1.5.2/1.5.2.json", "E:/1.5.2.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
