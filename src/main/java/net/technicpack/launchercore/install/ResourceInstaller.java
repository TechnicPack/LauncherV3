package net.technicpack.launchercore.install;

import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.util.DownloadUtils;
import net.technicpack.launchercore.util.MD5Utils;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;

public class ResourceInstaller {
	private Document resourceXML;
	private String url;
	private File assetsDirectory;

	public ResourceInstaller() {
		url = MojangConstants.assets;
		resourceXML = XMLUtils.getXMLFromURL(url);
		assetsDirectory = Utils.getAssetsDirectory();
	}

	public void updateResources() {
		NodeList contentList = resourceXML.getElementsByTagName("Contents");

		for (int i = 0; i < contentList.getLength(); i++) {
			Node content = contentList.item(i);
			Element eContent = (Element) content;

			String fileName = eContent.getElementsByTagName("Key").item(0).getTextContent();
			String fileSize = eContent.getElementsByTagName("Size").item(0).getTextContent();

			String rawETag = eContent.getElementsByTagName("ETag").item(0).getTextContent();
			String fileHash = rawETag.replace("\"", "");

			String fileURL = url + fileName;
			fileURL = fileURL.replace(" ", "%20");

			File fContent = new File(assetsDirectory, fileName);

			if (fileSize.equals("0")) {
				continue;
			}

			if (fContent.exists() && MD5Utils.checkMD5(fContent, fileHash)) {
				continue;
			}

			try {
				fContent.mkdirs();
				DownloadUtils.downloadFile(fileURL, fileName, fContent.getPath());
			} catch (IOException error) {
				Utils.getLogger().warning("Failed to download resource " + fileName);
			}

		}
	}
}
