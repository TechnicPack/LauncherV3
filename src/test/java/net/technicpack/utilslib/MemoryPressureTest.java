package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemoryPressureTest {
  @Test
  void linuxEstimateIncludesReclaimableMemoryRatherThanOnlyFreePages() throws Exception {
    try (BufferedReader reader =
        new BufferedReader(
            new StringReader(
                "MemTotal: 16777216 kB\nMemFree: 524288 kB\nMemAvailable: 8388608 kB\n"))) {
      assertEquals(8192, MemoryPressure.readLinuxAvailableMemory(reader));
    }
  }

  @Test
  void missingLinuxEstimateDoesNotSubstituteTotalOrFreeMemory() throws Exception {
    try (BufferedReader reader =
        new BufferedReader(new StringReader("MemTotal: 16777216 kB\nMemFree: 524288 kB\n"))) {
      long available = MemoryPressure.readLinuxAvailableMemory(reader);
      assertEquals(-1, available);
      assertFalse(MemoryPressure.shouldWarn(4096, available));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid kB", "-1024 kB", "8192 MB"})
  void malformedLinuxEstimateIsUnknown(String value) throws Exception {
    try (BufferedReader reader = new BufferedReader(new StringReader("MemAvailable: " + value))) {
      assertEquals(-1, MemoryPressure.readLinuxAvailableMemory(reader));
    }
  }

  @Test
  void zeroAvailableMemoryIsPressureRatherThanAnUnavailableReading() throws Exception {
    try (BufferedReader reader = new BufferedReader(new StringReader("MemAvailable: 0 kB"))) {
      assertTrue(MemoryPressure.shouldWarn(4096, MemoryPressure.readLinuxAvailableMemory(reader)));
    }
  }

  @Test
  void warningAllowsExactHeapAndHeadroomBudgetButWarnsBelowIt() {
    long budget = 4096 + MemoryPressure.HEADROOM_MB;
    assertFalse(MemoryPressure.shouldWarn(4096, budget));
    assertTrue(MemoryPressure.shouldWarn(4096, budget - 1));
  }
}
