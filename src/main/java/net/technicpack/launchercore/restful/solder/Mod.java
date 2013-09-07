package net.technicpack.launchercore.restful.solder;

public class Mod {
	private String name;
	private String version;
	private String url;
	private String md5;

	public Mod() {

	}

	public Mod(String name, String version, String url, String md5) {
		this.name = name;
		this.version = version;
		this.url = url;
		this.md5 = md5;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getUrl() {
		return url;
	}

	public String getMd5() {
		return md5;
	}

	@Override
	public String toString() {
		return "Mod{" +
				"name='" + name + '\'' +
				", version='" + version + '\'' +
				", url='" + url + '\'' +
				", md5='" + md5 + '\'' +
				'}';
	}
}
