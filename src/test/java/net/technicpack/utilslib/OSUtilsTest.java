package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

class OSUtilsTest {
  // JNA exposes each fixture as a native C-callable function pointer. These tests exercise
  // native argument widths and output buffers without claiming to emulate macOS or Rosetta.
  public interface Sysctl extends Callback {
    int invoke(String name, Pointer value, Pointer length, Pointer newValue, NativeLong newLength);
  }

  @Test
  void nativeArm64CapabilityOverridesAnX64JvmFallback() {
    assertTrue(query(fixture(0, 1, 4), false));
  }

  @Test
  void successfulIntelResultIsNotTreatedAsAnUnavailableQuery() {
    assertFalse(query(fixture(0, 0, 4), true));
  }

  @Test
  void failedQueryIgnoresOutputAndPreservesEitherJvmFallback() {
    assertFalse(query(fixture(-1, 1, 4), false));
    assertTrue(query(fixture(-1, 0, 4), true));
  }

  @Test
  void incompleteNativeResultDoesNotClaimArm64Support() {
    assertFalse(query(fixture(0, 1, 2), false));
  }

  @Test
  void unexpectedCapabilityValuePreservesJvmDetection() {
    assertFalse(query(fixture(0, 2, 4), false));
    assertTrue(query(fixture(0, 2, 4), true));
  }

  private static boolean query(Sysctl fixture, boolean fallback) {
    try {
      return OSUtils.queryMacArm64(
          Function.getFunction(CallbackReference.getFunctionPointer(fixture)), fallback);
    } finally {
      Reference.reachabilityFence(fixture);
    }
  }

  private static Sysctl fixture(int status, int capability, int returnedBytes) {
    return (name, value, length, newValue, newLength) -> {
      long capacity =
          Native.SIZE_T_SIZE == 8 ? length.getLong(0) : Integer.toUnsignedLong(length.getInt(0));
      if (!"hw.optional.arm64".equals(name)
          || capacity != 4
          || newValue != null
          || newLength.longValue() != 0) {
        return -1;
      }
      value.setInt(0, capability);
      if (Native.SIZE_T_SIZE == 8) length.setLong(0, returnedBytes);
      else length.setInt(0, returnedBytes);
      return status;
    };
  }
}
