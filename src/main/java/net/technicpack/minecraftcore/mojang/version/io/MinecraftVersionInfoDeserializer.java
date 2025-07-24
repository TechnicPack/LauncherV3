package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.*;

import java.lang.reflect.Type;

public class MinecraftVersionInfoDeserializer implements JsonDeserializer<MinecraftVersionInfo> {
    @Override
    public MinecraftVersionInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        // Safeguard in the event the version.json we get is actually the one inside a vanilla Minecraft jar
        // (which isn't valid)
        if (root.has("world_version") && root.has("protocol_version")) {
            throw new JsonSyntaxException("Invalid version file, this looks like a Minecraft client jar. Are you sure you didn't place a Minecraft jar as the modpack.jar?");
        }

        MinecraftVersionInfoRaw raw = context.deserialize(json, MinecraftVersionInfoRaw.class);

        return new MinecraftVersionInfo(raw);
    }
}
