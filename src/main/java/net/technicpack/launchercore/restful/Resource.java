package net.technicpack.launchercore.restful;

public class Resource {

	private String url;
	private String md5;

	public Resource() {

	}

	public Resource(String url, String md5) {
		this.url = url;
		this.md5 = md5;
	}

	public String getUrl() {
		return url;
	}

	public String getMd5() {
		return md5;
	}
}
