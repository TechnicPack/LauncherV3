package net.technicpack.discord.io;

import net.technicpack.rest.RestObject;

import java.util.List;

import java.util.List;

public class Server extends RestObject {
    private String id;
    private String name;
    private String instant_invite;
    private List<MemberInfo> members;
    private List<ChannelInfo> channels;

    public String getId() { return this.id; }
    public String getName() { return this.name; }
    public String getInviteLink() { return instant_invite; }

    public int getChannelCount() { return channels.size(); }
    public ChannelInfo getFirstChannel() {
        if (channels.size() == 0)
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
}
