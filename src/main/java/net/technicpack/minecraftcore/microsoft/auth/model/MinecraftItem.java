package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class MinecraftItem {
    @Key(value="name") public String name;
    @Key(value="signature") public String signature;
}
