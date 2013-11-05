package net.technicpack.launchercore.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.util.DownloadUtils;
import net.technicpack.launchercore.util.MD5Utils;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.XMLUtils;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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

		if (resourceXML == null)
			return;

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

		//Find and parse sounds.json - some /sounds/ folder sounds are actually copies from /sound/, or aren't listed
		//in the minecraft assets root.
		JsonObject soundsRoot = this.getSoundsJsonObject();

		if (soundsRoot == null) {
			Utils.getLogger().warning("Could not load & parse sounds.json.");
			return;
		}

		for (Map.Entry<String, JsonElement> entry : soundsRoot.entrySet()) {
			//For each item in the json, loop through and get the array of sounds
			JsonArray sounds = entry.getValue().getAsJsonObject().get("sounds").getAsJsonArray();

			for (JsonElement soundElement : sounds) {
				//For each sound in the array, if it's not already in /sounds/, see if it's a streaming sound
				//If so, try to download, if not, try to copy from /sound/

				String sound = null;
				boolean tryDownload = false;

				if (soundElement.isJsonPrimitive()) {
					sound = soundElement.getAsString();
				} else if (soundElement.getAsJsonObject().has("stream") && soundElement.getAsJsonObject().get("stream").getAsBoolean()) {
					sound = soundElement.getAsJsonObject().get("name").getAsString();
					tryDownload = true;
				}

				if (sound == null) {
					continue;
				}

				File soundDest = new File(assetsDirectory, "sounds/"+sound+".ogg");

				if (!soundDest.exists()) {
					if (tryDownload) {
						String fileURL = url + "sounds/" + sound + ".ogg";
						fileURL = fileURL.replace(" ", "%20");

						try {
							soundDest.mkdirs();
							DownloadUtils.downloadFile(fileURL, sound, soundDest.getPath());
						} catch (IOException ex) {
							Utils.getLogger().warning("Failed to download sound " + sound + " from the minecraft servers.");
						}
					} else {
						copyFile(sound, soundDest);
					}
				}
			}
		}
	}

	/**
	 * Attempt to parse the file sounds.json into a JsonObject and return the object.
	 *
	 * @return A JsonObject representing the contents of the sounds.json file, or null if we failed to load & parse.
	 */
	private JsonObject getSoundsJsonObject() {
		File soundsJson = new File(assetsDirectory, "sounds.json");

		if (soundsJson.exists()) {
			String soundsData = null;

			try {
				//Pull json from file
				soundsData = FileUtils.readFileToString(soundsJson, Charset.forName("UTF-8"));
			} catch (IOException e) {
				Utils.getLogger().warning("Failed to open sounds.json for processing.");
			}

			if (soundsData != null) {
				//Parse into json tree
				JsonParser parser = new JsonParser();
				return (JsonObject) parser.parse(soundsData);
			}
		}

		return null;
	}

	/**
	 * Attempt to copy a file from the /sound/ folder to a target destination.  Used to copy equivalent
	 * files from /sound/ to /sounds/.
	 *
	 * @param fileName The name of the file in the /sound/ folder to be copied.
	 * @param destination The target destination to copy to.
	 */
	private void copyFile(String fileName, File destination) {
		File source = new File(assetsDirectory, "sound/"+fileName+".ogg");

		if (source.exists()) {
			try {
				FileUtils.copyFile(source, destination);
			} catch (IOException ex) {
				Utils.getLogger().warning("Error attempting to copy "+fileName+" from /sound/ folder.");
			}
		} else {
			Utils.getLogger().warning("Could not locate source file for "+fileName+" in /sound/ folder.");
		}
	}
}