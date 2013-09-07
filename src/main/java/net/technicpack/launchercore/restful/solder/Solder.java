package net.technicpack.launchercore.restful.solder;

import net.technicpack.launchercore.restful.RestObject;

import java.util.Map;

public class Solder extends RestObject {
	private Map<String, String> modpacks;
	private String mirror_url;

	public Map<String, String> getModpacks() {
		return modpacks;
	}

	public String getMirrorUrl() {
		return mirror_url;
	}
}
