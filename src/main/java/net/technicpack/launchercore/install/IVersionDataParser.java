package net.technicpack.launchercore.install;

/**
 * Created by Stephen on 8/16/2014.
 */
public interface IVersionDataParser<VersionData> {
    VersionData parseVersionData(String data);
}
