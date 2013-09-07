package net.technicpack.launchercore.restful.platform;

import net.technicpack.launchercore.restful.RestObject;

import java.util.List;

public class News extends RestObject {
	private List<Article> news;

	public List<Article> getNews() {
		return news;
	}
}
