package net.technicpack.launcher.launch;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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
    assertThrows(
        InterruptedException.class, () -> ImmutableInstallerPlanner.throwIfCancelled(() -> true));
  }
}
