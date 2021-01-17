package net.technicpack.minecraftcore.live.auth.response;

public class MinecraftProfile {
    private String id;
    private String name;
    private MinecraftProfileSkin[] skins;
    // "capes": []

    public String GetID() { return id; }
    public String GetName() { return name; }
    public MinecraftProfileSkin[] GetSkins() { return skins; }
}
