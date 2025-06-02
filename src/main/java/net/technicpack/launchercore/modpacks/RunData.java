package net.technicpack.launchercore.modpacks;

import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.utilslib.Memory;
import net.technicpack.utilslib.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

@SuppressWarnings({"unused"})
public class RunData {
    private String java;
    private String memory;
    transient private long memoryLong = -1;

    public RunData() {}

    public String getJava() { return java; }

    public long getMemory() {
        if (memoryLong != -1) {
            return memoryLong;
        }

        if (memory == null || memory.isEmpty()) {
            memoryLong = 0;
        } else {
            try {
                if (memory.charAt(0) == '-') {
                    throw new NumberFormatException("Invalid runData memory specified, cannot be negative");
                }
                memoryLong = Long.parseLong(memory);
            } catch (NumberFormatException ex) {
                Utils.getLogger().log(Level.WARNING, "Exception caught when parsing runData memory", ex);
                memoryLong = 0;
            }
        }

        return memoryLong;
    }

    public Memory getMemoryObject() {
        return getMemorySetting(getMemory());
    }

    public static boolean isJavaVersionAtLeast(String testString, String compareString) {
        String[] compareVersion = compareString.split("[._]");
        String[] testVersion = testString.split("[._]");

        int compareLength = Math.min(compareVersion.length, testVersion.length);
        for (int i = 0; i < compareLength; i++) {
            int refVal = Integer.parseInt(compareVersion[i]);
            int testVal = Integer.parseInt(testVersion[i]);

            if (refVal == testVal)
                continue;
            return testVal > refVal;
        }

        if (compareVersion.length == testVersion.length)
            return true;
        return compareVersion.length < testVersion.length;
    }

    public boolean isJavaValid(String testString) {
        String compareString = java;
        if (compareString == null || compareString.length() == 0)
            compareString = "1.6";

        return isJavaVersionAtLeast(testString, compareString);
    }

    public boolean isMemoryValid(long memory) {
        return getMemory() <= memory;
    }

    public boolean isRunDataValid(long memory, String java, boolean usingMojangJava) {
        return isMemoryValid(memory) && (usingMojangJava || isJavaValid(java));
    }

    public IJavaRuntime getValidJavaVersion(JavaVersionRepository repository) {

        boolean requires64Bit = getMemory() > Memory.MAX_32_BIT_MEMORY;

        IJavaRuntime best64Bit = repository.getBest64BitVersion();
        if (best64Bit != null && isJavaValid(best64Bit.getVersion()))
            return best64Bit;
        else if (best64Bit == null && requires64Bit)
            return null;

        IJavaRuntime bestVersion = null;
        for (IJavaRuntime version : repository.getVersions()) {
            if (isJavaValid(version.getVersion()) && (!requires64Bit || version.is64Bit()))
                bestVersion = version;
        }

        return bestVersion;
    }

    public Memory getValidMemory(JavaVersionRepository repository) {
        boolean can64Bit = (repository.getBest64BitVersion() != null);
        Memory required = getMemorySetting(getMemory());
        Memory available = Memory.getClosestAvailableMemory(required, can64Bit);

        if (available.getMemoryMB() < required.getMemoryMB())
            return null;

        return available;
    }

    private Memory getMemorySetting(long memory) {
        for (Memory setting : Memory.memoryOptions) {
            if (setting.getMemoryMB() >= memory)
                return setting;
        }

        // Handle case where modpack specifies more memory than is possible for the launcher
        Memory maxMemoryOption = Arrays.stream(Memory.memoryOptions).max(Comparator.comparingLong(Memory::getMemoryMB)).orElseThrow(() -> new RuntimeException("No memory options available"));
        Utils.getLogger().log(Level.WARNING, "Invalid runData memory value specified (" + memory + " MB); using maximum possible instead (" + maxMemoryOption.getMemoryMB() + " MB)");
        return maxMemoryOption;
    }
}
