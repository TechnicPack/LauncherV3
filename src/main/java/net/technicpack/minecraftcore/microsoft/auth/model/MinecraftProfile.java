package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

/**
 *
 */
public class MinecraftProfile {
    @Key(value="id") public String id;
    @Key(value="name") public String name;

    @Override
    public String toString() {
        return "MinecraftProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
