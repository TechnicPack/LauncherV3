package net.technicpack.launchercore.restful.platform;

import net.technicpack.launchercore.restful.Resource;

public class Article {
	private String title;
	private String displayTitle;
	private Resource image;
	private String category;
	private String user;
	private String summary;
	private String date;

	public String getTitle() {
		return title;
	}

	public String getDisplayTitle() {
		return displayTitle;
	}

	public Resource getImage() {
		return image;
	}

	public String getCategory() {
		return category;
	}

	public String getUser() {
		return user;
	}

	public String getSummary() {
		return summary;
	}

	public String getDate() {
		return date;
	}
}
