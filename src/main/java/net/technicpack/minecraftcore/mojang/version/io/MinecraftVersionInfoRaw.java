package net.technicpack.minecraftcore.mojang.version.io;

import java.util.List;

/**
 * POJO for use with {@link MinecraftVersionInfoDeserializer} and {@link MinecraftVersionInfo}.
 * <p>
 * This is the raw deserialized form of <a href="https://minecraft.wiki/w/Client.json">client.json</a>.
 */
class MinecraftVersionInfoRaw {
    String id;
    ReleaseType type;
    LaunchArguments arguments; // Modern (minimumLauncherVersion >= 21)
    String minecraftArguments; // Legacy (minimumLauncherVersion < 21)
    List<Library> libraries;
    String mainClass;
    List<Rule> rules;
    String assets;
    AssetIndex assetIndex;
    GameDownloads downloads;
    String inheritsFrom;
    VersionJavaInfo javaVersion;
}
