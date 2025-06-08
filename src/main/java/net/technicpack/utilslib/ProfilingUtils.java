package net.technicpack.utilslib;

public class ProfilingUtils {
    /**
     * Measure the time it takes to run a runnable and log the result to the logger.
     */
    public static void measureTime(String description, Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        long duration = System.nanoTime() - start;
        Utils.getLogger().fine(String.format("%s took %s ms", description, duration / 1_000_000.0));
    }
}
