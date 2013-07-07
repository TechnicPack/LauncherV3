package net.technicpack.launcher.restful.technic;

public class PlatformPackInfo implements PackInfo {
	private String name;
	private String displayName;
	private String url;
	private ImageResource icon;
	private ImageResource logo;
	private ImageResource background;
	private String minecraft;
	private String forge;
	private String build;
	private String solder;

	@Override
	public ImageResource getBackground() {
		return background;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public ImageResource getIcon() {
		return icon;
	}

	@Override
	public String getLatest() {
		return build;
	}

	@Override
	public ImageResource getLogo() {
		return logo;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getRecommended() {
		return build;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public String getMinecraft() {
		return minecraft;
	}

	public String getForge() {
		return forge;
	}

	public String getSolder() {
		return solder;
	}

	public boolean hasSolder() {
		return solder == null || solder.equals("");
	}
}
