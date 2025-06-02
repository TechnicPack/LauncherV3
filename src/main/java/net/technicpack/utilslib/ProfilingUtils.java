package net.technicpack.utilslib;

import java.util.concurrent.Callable;

public class ProfilingUtils {
    /**
     * Measure the time it takes to run a runnable and log the result to the logger.
     * @param description
     * @param runnable
     */
    public static void measureTime(String description, Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        long duration = System.nanoTime() - start;
        Utils.getLogger().fine(() -> description + " took " + (duration / 1_000_000.0) + " ms");
    }
}
