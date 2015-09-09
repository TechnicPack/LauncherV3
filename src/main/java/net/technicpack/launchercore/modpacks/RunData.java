package net.technicpack.launchercore.modpacks;

import net.technicpack.launchercore.launch.java.IJavaVersion;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.utilslib.Memory;

public class RunData {
    private String java;
    private String memory;

    public RunData() {}

    public String getJava() { return java; }
    public long getMemory() {
        try {
            return Long.parseLong(memory);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public Memory getMemoryObject() {
        return getMemorySetting(getMemory());
    }

    public static boolean isJavaVersionAtLeast(String testString, String compareString) {
        String[] compareVersion = compareString.split("[._]");
        String[] testVersion = testString.split("[._]");

        int compareLength = Math.min(compareVersion.length, testVersion.length);
        for (int i = 0; i < compareLength; i++) {
            int compare = compareVersion[i].compareTo(testVersion[i]);

            if (compare == 0)
                continue;
            return compare < 0;
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

    public boolean isRunDataValid(long memory, String java) {
        return isMemoryValid(memory) && isJavaValid(java);
    }

    public IJavaVersion getValidJavaVersion(JavaVersionRepository repository) {

        boolean requires64Bit = getMemory() > Memory.MAX_32_BIT_MEMORY;

        IJavaVersion best64Bit = repository.getBest64BitVersion();
        if (best64Bit != null && isJavaValid(best64Bit.getVersionNumber()))
            return best64Bit;
        else if (best64Bit == null && requires64Bit)
            return null;

        IJavaVersion bestVersion = null;
        for (IJavaVersion version : repository.getVersions()) {
            if (isJavaValid(version.getVersionNumber()) && (!requires64Bit || version.is64Bit()))
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

        return null;
    }
}
