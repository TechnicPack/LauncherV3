package net.technicpack.minecraftcore.launch;

import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.ALREADY_MAXIMIZED;
import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.CONFIRMED;
import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.CONFIRM_NANOS;
import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.REQUEST;
import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.STABLE_NANOS;
import static net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy.WAIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Memory;
import net.technicpack.minecraftcore.launch.WindowsWindowMaximizer.WindowPolicy;
import org.junit.jupiter.api.Test;

class WindowsWindowMaximizerTest {
  @Test
  void acceptsOnlyExactGameWindowClassesNotArbitraryNonAwtWindows() {
    try (Memory name = new Memory(512)) {
      for (String expected : new String[] {"LWJGL", "GLFW30", "SDL_app"}) {
        putClassName(name, expected);
        assertTrue(WindowsWindowMaximizer.isGameClass(name, expected.length()));
      }
      for (String rejected :
          new String[] {
            "",
            "SunAwtFrame",
            "java",
            "ConsoleWindowClass",
            "GLFW",
            "GLFW30Splash",
            "LWJGL2",
            "SDL",
            "SDL_appSplash",
            "sdl_app"
          }) {
        putClassName(name, rejected);
        assertFalse(WindowsWindowMaximizer.isGameClass(name, rejected.length()), rejected);
      }
      assertFalse(WindowsWindowMaximizer.isGameClass(name, -1));
    }
  }

  @Test
  void requiresBothResizableAndMaximizeStylesIncludingWithHighStyleBitSet() {
    assertFalse(WindowsWindowMaximizer.isMaximizableStyle(0));
    assertFalse(WindowsWindowMaximizer.isMaximizableStyle(0x00040000));
    assertFalse(WindowsWindowMaximizer.isMaximizableStyle(0x00010000));
    assertTrue(WindowsWindowMaximizer.isMaximizableStyle(0x00050000));
    assertTrue(WindowsWindowMaximizer.isMaximizableStyle(0x80050000));
  }

  @Test
  void rejectsInvalidIdentitiesBeforeLoadingWindowsLibraries() {
    for (long pid : new long[] {0, -1, 0x100000000L}) {
      assertThrows(IllegalArgumentException.class, () -> WindowsWindowMaximizer.open(pid, 10));
    }
    assertThrows(IllegalArgumentException.class, () -> WindowsWindowMaximizer.open(10, 0));
    assertThrows(IllegalArgumentException.class, () -> WindowsWindowMaximizer.open(10, -1));
  }

  @Test
  void permitsChildrenOfExitedParentsButRejectsReusedParentPidChronology() {
    assertTrue(WindowsWindowMaximizer.createdDuringParentLifetime(100, 0, 150));
    assertTrue(WindowsWindowMaximizer.createdDuringParentLifetime(100, 200, 150));
    assertTrue(WindowsWindowMaximizer.createdDuringParentLifetime(100, 200, 100));
    assertTrue(WindowsWindowMaximizer.createdDuringParentLifetime(100, 200, 200));
    assertFalse(WindowsWindowMaximizer.createdDuringParentLifetime(100, 0, 99));
    assertFalse(WindowsWindowMaximizer.createdDuringParentLifetime(100, 200, 201));
  }

  @Test
  void slowStartupHasNoOverallDeadlineAndStillRequiresStableWindow() {
    WindowPolicy policy = new WindowPolicy();
    emptySweep(policy);
    long muchLater = 86_400_000_000_000L;
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, muchLater));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, muchLater + STABLE_NANOS - 1));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, muchLater + STABLE_NANOS));
  }

  @Test
  void shortLivedSplashDoesNotConsumeTheGamesMaximization() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, STABLE_NANOS - 1));
    assertEquals(WAIT, sweep(policy, 2, 10, true, false, STABLE_NANOS));
    assertEquals(REQUEST, sweep(policy, 2, 10, true, false, 2 * STABLE_NANOS));
  }

  @Test
  void hiddenOrMinimizedPeriodRestartsStabilityBeforeAnyRequest() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(WAIT, sweep(policy, 1, 10, false, false, STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 2 * STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 3 * STABLE_NANOS - 1));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, 3 * STABLE_NANOS));
  }

  @Test
  void postedRequestIsNotSuccessUntilZoomedIsObserved() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, STABLE_NANOS + 1));
    assertEquals(CONFIRMED, sweep(policy, 1, 10, true, true, STABLE_NANOS + 2));
  }

  @Test
  void existingMaximizedWindowDoesNotClaimWeMaximizedIt() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, true, 0));
    assertEquals(ALREADY_MAXIMIZED, sweep(policy, 1, 10, true, true, STABLE_NANOS));
  }

  @Test
  void failedPostingNeverRetriesASurvivingWindowEvenAfterEligibilityChanges() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    policy.requestFailed(1);
    assertEquals(WAIT, sweep(policy, 1, 10, true, true, STABLE_NANOS + 1));
    assertEquals(WAIT, sweep(policy, 1, 10, false, false, 2 * STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 3 * STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 4 * STABLE_NANOS));
  }

  @Test
  void confirmationExpiresWithoutRetryingOrReportingLateSuccess() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    long expired = STABLE_NANOS + CONFIRM_NANOS;
    assertEquals(WAIT, sweep(policy, 1, 10, true, true, expired));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, expired + STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, true, expired + 2 * STABLE_NANOS));
  }

  @Test
  void aFailedCandidateDoesNotPreventDiscoveringAnotherGameWindow() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    policy.requestFailed(1);
    policy.beginSweep();
    assertEquals(WAIT, policy.observe(1, 10, true, false, 2 * STABLE_NANOS));
    assertEquals(WAIT, policy.observe(2, 10, true, false, 2 * STABLE_NANOS));
    policy.endSweep();
    policy.beginSweep();
    assertEquals(WAIT, policy.observe(1, 10, true, false, 3 * STABLE_NANOS));
    assertEquals(REQUEST, policy.observe(2, 10, true, false, 3 * STABLE_NANOS));
    policy.endSweep();
  }

  @Test
  void destructionDuringConfirmationAllowsAReplacementWithTheSameHandle() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    emptySweep(policy);
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 2 * STABLE_NANOS));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, 3 * STABLE_NANOS));
  }

  @Test
  void reusedHandleInAnotherProcessCannotConfirmThePreviousRequest() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 20, true, true, STABLE_NANOS + 1));
    assertEquals(ALREADY_MAXIMIZED, sweep(policy, 1, 20, true, true, 2 * STABLE_NANOS + 1));
  }

  @Test
  void failedPidQueryDoesNotForgetAnAttemptOnASurvivingHandle() {
    WindowPolicy policy = new WindowPolicy();
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 0));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, STABLE_NANOS));
    policy.requestFailed(1);
    assertEquals(WAIT, sweep(policy, 1, 0, false, false, 2 * STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 3 * STABLE_NANOS));
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, 4 * STABLE_NANOS));
  }

  @Test
  void monotonicClockWrapDoesNotBreakStabilityOrConfirmation() {
    WindowPolicy policy = new WindowPolicy();
    long start = Long.MAX_VALUE - STABLE_NANOS / 2;
    assertEquals(WAIT, sweep(policy, 1, 10, true, false, start));
    assertEquals(REQUEST, sweep(policy, 1, 10, true, false, start + STABLE_NANOS));
    assertEquals(CONFIRMED, sweep(policy, 1, 10, true, true, start + STABLE_NANOS + 1));
  }

  private static int sweep(
      WindowPolicy policy, long window, long pid, boolean eligible, boolean zoomed, long now) {
    policy.beginSweep();
    int action = policy.observe(window, pid, eligible, zoomed, now);
    policy.endSweep();
    return action;
  }

  private static void emptySweep(WindowPolicy policy) {
    policy.beginSweep();
    policy.endSweep();
  }

  private static void putClassName(Memory memory, String name) {
    for (int i = 0; i < name.length(); i++) memory.setShort(i * 2L, (short) name.charAt(i));
  }
}
