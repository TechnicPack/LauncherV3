package net.technicpack.platform.io;

import java.util.Date;

public class FeedItem {
    private String user;
    private long date;
    private String content;
    private String avatar;
    private String url;

    public FeedItem() {}

    public String getUser() { return user; }
    public Date getDate() { return new Date(date*1000); }
    public String getContent() { return content; }
    public String getAvatar() { return avatar; }
    public String getUrl() { return url; }
}
