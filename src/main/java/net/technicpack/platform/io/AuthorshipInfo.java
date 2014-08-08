package net.technicpack.platform.io;

import java.util.Date;

public class AuthorshipInfo {
    private String user;
    private String avatar;
    private Date date;

    public AuthorshipInfo() {}
    public AuthorshipInfo(String user, String avatar, Date date) {
        this.user = user;
        this.avatar = avatar;
        this.date = date;
    }

    public String getUser() { return user; }
    public String getAvatar() { return avatar; }
    public Date getDate() { return date; }
}
