package net.technicpack.minecraftcore.mojang.java;

import com.google.gson.annotations.SerializedName;

public enum JavaRuntimeFileType {
    @SerializedName("directory")
    DIRECTORY,

    @SerializedName("file")
    FILE,

    @SerializedName("link")
    LINK,
}
