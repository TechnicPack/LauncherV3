package net.technicpack.platform.io;

import java.util.Date;

public class NewsArticle {
    private int id;
    private String username;
    private String avatar;
    private String title;
    private String content;
    private long date;

    public NewsArticle() {

    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Date getDate() { return new Date(date*1000); }
    public AuthorshipInfo getAuthorshipInfo() { return new AuthorshipInfo(getUsername(), getAvatar(), getDate()); }
}
