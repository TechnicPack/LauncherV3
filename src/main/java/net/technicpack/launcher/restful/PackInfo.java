package net.technicpack.launcher.restful;


public interface PackInfo {

	public String getName();

	public String getDisplayName();

	public String getUrl();

	public ImageResource getIcon();

	public ImageResource getBackground();

	public ImageResource getLogo();

	public String getRecommended();

	public String getLatest();
}
