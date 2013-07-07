package net.technicpack.launcher.restful.technic;

import java.util.List;

public class SolderPackInfo implements PackInfo {

	private String name;
	private String displayName;
	private String url;
	private ImageResource icon;
	private ImageResource logo;
	private ImageResource background;
	private String recommended;
	private String latest;
	private List<String> builds;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public ImageResource getIcon() {
		return icon;
	}

	@Override
	public ImageResource getLogo() {
		return logo;
	}

	@Override
	public ImageResource getBackground() {
		return background;
	}

	@Override
	public String getRecommended() {
		return recommended;
	}

	@Override
	public String getLatest() {
		return latest;
	}

	public List<String> getBuilds() {
		return builds;
	}
}
