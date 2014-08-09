package net.technicpack.platform.io;

import net.technicpack.rest.RestObject;

import java.util.ArrayList;

public class NewsData extends RestObject {
    private ArrayList<NewsArticle> articles = new ArrayList<NewsArticle>();

    public ArrayList<NewsArticle> getArticles() { return articles; }
}
