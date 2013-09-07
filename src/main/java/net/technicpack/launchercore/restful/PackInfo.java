package net.technicpack.launchercore.restful;


public interface PackInfo {

	public String getName();

	public String getDisplayName();

	public String getUrl();

	public Resource getIcon();

	public Resource getBackground();

	public Resource getLogo();

	public String getRecommended();

	public String getLatest();
}
