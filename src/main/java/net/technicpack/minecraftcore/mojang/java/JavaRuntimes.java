package net.technicpack.minecraftcore.mojang.java;

import com.google.gson.annotations.SerializedName;
import net.technicpack.utilslib.JavaUtils;
import net.technicpack.utilslib.OperatingSystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaRuntimes {
    @SerializedName("linux")
    private Map<String, List<JavaRuntime>> linux64;
    @SerializedName("linux-i386")
    private Map<String, List<JavaRuntime>> linux32;

    @SerializedName("mac-os")
    private Map<String, List<JavaRuntime>> mac;

    @SerializedName("mac-os-arm64")
    private Map<String, List<JavaRuntime>> macArm64;

    @SerializedName("windows-x64")
    private Map<String, List<JavaRuntime>> windows64;
    @SerializedName("windows-x86")
    private Map<String, List<JavaRuntime>> windows32;

    public Map<String, List<JavaRuntime>> getRuntimesForCurrentOS() {
        switch (OperatingSystem.getOperatingSystem()) {
            case WINDOWS:
                if (JavaUtils.is64Bit()) {
                    return windows64;
                }

                return windows32;
            case OSX:
                if (JavaUtils.isArm64()) {
                    // Combine the arm64 and x64 entries, with the arm64 ones taking precedence
                    Map<String, List<JavaRuntime>> combinedMac = new HashMap<>(mac);
                    macArm64.forEach((key, value) -> {
                        if (!value.isEmpty()) {
                            combinedMac.put(key, value);
                        }
                    });

                    return combinedMac;
                }

                return mac;
            case LINUX:
                if (JavaUtils.is64Bit()) {
                    return linux64;
                }

                return linux32;
        }

        return null;
    }

    public JavaRuntime getRuntimeForCurrentOS(String runtimeName) {
        Map<String, List<JavaRuntime>> availableRuntimes = getRuntimesForCurrentOS();

        if (availableRuntimes == null) return null;

        // For some reason every runtime is a list with a single entry
        return availableRuntimes.get(runtimeName).get(0);
    }
}
