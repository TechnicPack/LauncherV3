package net.technicpack.discord.io;

import com.google.gson.annotations.SerializedName;
import net.technicpack.rest.RestObject;

import java.util.List;

@SuppressWarnings({"unused"})
public class Server extends RestObject {
    private String id;
    private String name;
    @SerializedName("instant_invite")
    private String instantInvite;
    private List<MemberInfo> members;
    private List<ChannelInfo> channels;
    @SerializedName("presence_count")
    private int presenceCount;

    public String getId() { return this.id; }
    public String getName() { return this.name; }
    public String getInviteLink() { return instantInvite; }

    public int getChannelCount() { return channels.size(); }
    public ChannelInfo getFirstChannel() {
        if (channels.isEmpty())
            return null;
        return channels.get(0);
    }
    public ChannelInfo getChannel(int index) {
        return channels.get(index);
    }

    public int getMemberCount() { return members.size(); }
    public MemberInfo getMember(int index) {
        return members.get(index);
    }
    public int getPresenceCount() { return presenceCount; }
}
