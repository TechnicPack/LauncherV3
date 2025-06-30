package net.technicpack.utilslib;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProcessUtils {
    private static final String[] ENV_VARS_TO_REMOVE = new String[]{
            "JAVA_ARGS", "CLASSPATH", "CONFIGPATH", "JAVA_HOME", "JRE_HOME",
            "_JAVA_OPTIONS", "JAVA_OPTIONS", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS",
    };

    private ProcessUtils() {
        // Prevent instantiation of this utility class
    }

    /**
     * Creates a ProcessBuilder with the specified commands and removes certain
     * environment variables that may interfere with the process execution.
     *
     * @param command the list of commands to execute
     * @return a ProcessBuilder configured with the provided commands and cleaned
     *         environment variables
     */
    public static ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();

        for (String varName : ENV_VARS_TO_REMOVE) {
            env.remove(varName);
        }

        return pb;
    }

    public static ProcessBuilder createProcessBuilder(List<String> command, boolean allowJavaHome) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();

        for (String varName : ENV_VARS_TO_REMOVE) {
            if (varName.equals("JAVA_HOME") && allowJavaHome) {
                continue;
            }
            env.remove(varName);
        }

        return pb;
    }

    /**
     * @see #createProcessBuilder(List)
     */
    public static ProcessBuilder createProcessBuilder(String... command) {
        return createProcessBuilder(Arrays.asList(command));
    }
}
