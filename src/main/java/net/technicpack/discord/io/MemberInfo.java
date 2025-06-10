package net.technicpack.discord.io;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused"})
public class MemberInfo {
    private String username;
    private String status;
    @SerializedName( "avatar_url")
    private String avatarUrl;
    private String avatar;
    private String id;
    private String discriminator;
    private GameInfo game;
}
