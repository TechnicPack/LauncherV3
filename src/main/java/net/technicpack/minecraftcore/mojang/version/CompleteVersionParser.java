package net.technicpack.minecraftcore.mojang.version;

import net.technicpack.launchercore.install.IVersionDataParser;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersion;

public class CompleteVersionParser implements IVersionDataParser<CompleteVersion> {

    @Override
    public CompleteVersion parseVersionData(String data) {
        return MojangUtils.getGson().fromJson(data, CompleteVersion.class);
    }
}
