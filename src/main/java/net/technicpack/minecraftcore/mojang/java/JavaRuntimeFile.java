package net.technicpack.minecraftcore.mojang.java;

public class JavaRuntimeFile {
    private JavaRuntimeFileType type;
    private JavaRuntimeFileDownloads downloads;
    private boolean executable;

    // For "link" types, only this field exists. In other types, this field doesn't exist.
    // Seems "link" only exists for Linux and Mac JREs
    private String target;

    public JavaRuntimeFileType getType() {
        return type;
    }

    public JavaRuntimeFileDownloads getDownloads() {
        return downloads;
    }

    public boolean isExecutable() {
        return executable;
    }

    public String getTarget() {
        return target;
    }
}
