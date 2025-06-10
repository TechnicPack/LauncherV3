package net.technicpack.utilslib;

public class OSUtils {
    /**
     * Indicates whether the current process is running under WOW64 (Windows 32-bit
     * on Windows 64-bit).
     * This is determined by checking for the presence of the PROCESSOR_ARCHITEW6432
     * environment variable,
     * which is only set when a 32-bit process is running on a 64-bit Windows
     * system.
     */
    private static final boolean IS_WOW64_PROCESS;

    static {
        IS_WOW64_PROCESS = OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
                && System.getenv("PROCESSOR_ARCHITEW6432") != null;
    }

    private OSUtils() {
        // Prevent instantiation of this utility class
    }

    /**
     * Checks if the current process is running under WOW64 (Windows 32-bit on
     * Windows 64-bit).
     * WOW64 is the subsystem that allows 32-bit Windows applications to run on
     * 64-bit Windows.
     *
     * @return true if this is a 32-bit process running on 64-bit Windows, false
     *         otherwise
     */
    public static boolean isWow64Process() {
        return IS_WOW64_PROCESS;
    }

    /**
     * Checks if the current operating system is 64-bit.
     * This is determined by checking the system architecture property.
     * <p>
     * NOTE: ARM64 is also considered as 64-bit.
     *
     * @return true if the OS is 64-bit, false otherwise
     */
    public static boolean is64BitOS() {
        if (isWow64Process()) {
            return true;
        }
        String arch = System.getProperty("os.arch");
        return arch.contains("64");
    }
}
