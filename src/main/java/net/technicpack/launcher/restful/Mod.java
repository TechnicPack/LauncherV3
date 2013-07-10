package net.technicpack.launcher.restful;

public class Mod {
	private String name;
	private String version;
	private String md5;
	private String url;
	private String displayName;
	private String donate;
	private String author;

	public String getAuthor() {
		return author;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDonate() {
		return donate;
	}

	public String getMd5() {
		return md5;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getVersion() {
		return version;
	}
}
