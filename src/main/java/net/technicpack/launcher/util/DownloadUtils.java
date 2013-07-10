package net.technicpack.launcher.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import net.technicpack.launcher.exception.DownloadException;
import net.technicpack.launcher.util.Download.Result;

public class DownloadUtils {
	private static final int DOWNLOAD_RETRIES = 3;

	public static Download downloadFile(String url, String output, File cache, String md5, DownloadListener listener) throws IOException {
		int tries = DOWNLOAD_RETRIES;
		File outputFile = null;
		Download download = null;
		while (tries > 0) {
			System.out.println("Starting download of " + url + ", with " + tries + " tries remaining");
			tries--;
			download = new Download(url, output);
			download.setListener(listener);
			download.run();
			if (download.getResult() != Result.SUCCESS) {
				if (download.getOutFile() != null) {
					download.getOutFile().delete();
				}
				System.err.println("Download of " + url + " Failed!");
				if (listener != null) {
					listener.stateChanged("Download failed, retries remaining: " + tries, 0F);
				}
			} else {
				if (md5 != null) {
					String resultMD5 = MD5Utils.getMD5(download.getOutFile());
					System.out.println("Expected MD5: " + md5 + " Calculated MD5: " + resultMD5);
					if (md5.equalsIgnoreCase(resultMD5)) {
						outputFile = download.getOutFile();
						break;
					}
				} else {
					outputFile = download.getOutFile();
					break;
				}
			}
		}
		if (outputFile == null) {
			throw new DownloadException("Failed to download " + url, download != null ? download.getException() : null);
		}
		if (cache != null) {
			FileUtils.copyFile(outputFile, cache);
		}
		return download;
	}

	public static Download downloadFile(String url, String output, File cache) throws IOException {
		return downloadFile(url, output, cache, null, null);
	}

	public static Download downloadFile(String url, String output) throws IOException {
		return downloadFile(url, output, null);
	}
}
