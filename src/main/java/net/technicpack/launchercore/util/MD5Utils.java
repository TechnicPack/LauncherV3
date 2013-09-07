package net.technicpack.launchercore.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;

public class MD5Utils {

	public static String getMD5(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			String md5 = DigestUtils.md5Hex(fis);
			fis.close();
			return md5;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean checkMD5(String md5, String otherMd5) {
		return md5.equalsIgnoreCase(otherMd5);
	}
}
