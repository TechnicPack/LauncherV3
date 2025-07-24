package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.*;

import java.lang.reflect.Type;

public class MinecraftVersionInfoDeserializer implements JsonDeserializer<MinecraftVersionInfo> {
    // These are used by older formats (minimumLauncherVersion < 21)
    private static final String LEGACY_MINECRAFT_ARGUMENTS_KEY = "minecraftArguments";
    private static final String LEGACY_JAVA_ARGUMENTS_KEY = "javaArguments";

    @Override
    public MinecraftVersionInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        // Safeguard in the event the version.json we get is actually the one inside a vanilla Minecraft jar
        // (which isn't valid)
        if (root.has("world_version") && root.has("protocol_version")) {
            throw new JsonSyntaxException("Invalid version file, this looks like a Minecraft client jar. Are you sure you didn't place a Minecraft jar as the modpack.jar?");
        }

        MinecraftVersionInfo version = context.deserialize(json, MinecraftVersionInfo.class);

        if (root.has(LEGACY_MINECRAFT_ARGUMENTS_KEY)) {
            String minecraftArguments = root.get(LEGACY_MINECRAFT_ARGUMENTS_KEY).getAsString();
            String javaArguments = root.has(LEGACY_JAVA_ARGUMENTS_KEY) ? root.get(LEGACY_JAVA_ARGUMENTS_KEY).getAsString() : "";

            version.setArguments(LaunchArguments.fromLegacyString(minecraftArguments, javaArguments));
        }

        return version;
    }
}
