package net.technicpack.launcher.launch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ImmutableInstallerPlannerTest {
    @Test
    void throwIfInterruptedThrowsWhenCurrentThreadIsInterrupted() {
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedException.class, ImmutableInstallerPlanner::throwIfInterrupted);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void throwIfCancelledThrowsWhenCancellationFlagIsSet() {
        assertThrows(InterruptedException.class,
                () -> ImmutableInstallerPlanner.throwIfCancelled(() -> true));
    }
}
