package net.technicpack.launcher.restful.technic;

import java.util.List;

public class Minecraft {
	private String version;
	private String url;
	private String md5;
	private List<String> forge;

	public String getVersion() {
		return version;
	}

	public String getUrl() {
		return url;
	}

	public String getMd5() {
		return md5;
	}

	public List<String> getForge() {
		return forge;
	}
}
