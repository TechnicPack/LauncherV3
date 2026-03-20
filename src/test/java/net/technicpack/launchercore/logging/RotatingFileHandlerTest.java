package net.technicpack.launchercore.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RotatingFileHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void closeStopsBackgroundLoggingThread() throws Exception {
        RotatingFileHandler handler = new RotatingFileHandler(tempDir, "techniclauncher_%s.log");
        Thread loggingThread = getLoggingThread(handler);

        handler.close();

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            while (loggingThread.isAlive()) {
                Thread.sleep(10);
            }
        });
    }

    private static Thread getLoggingThread(RotatingFileHandler handler) throws ReflectiveOperationException {
        Field field = RotatingFileHandler.class.getDeclaredField("loggingThread");
        field.setAccessible(true);
        return (Thread) field.get(handler);
    }
}
