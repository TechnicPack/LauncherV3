package net.technicpack.launchercore.restful.platform;

import net.technicpack.launchercore.restful.Resource;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.RestObject;

public class PlatformPackInfo extends RestObject implements PackInfo {
	private String name;
	private String displayName;
	private String url;
	private Resource icon;
	private Resource logo;
	private Resource background;
	private String minecraft;
	private String forge;
	private String build;
	private String solder;

	@Override
	public Resource getBackground() {
		return background;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Resource getIcon() {
		return icon;
	}

	@Override
	public String getLatest() {
		return build;
	}

	@Override
	public Resource getLogo() {
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
