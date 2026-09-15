package net.technicpack.utilslib;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import java.util.logging.Level;

public class OSUtils {
  private OSUtils() {
    // Prevent instantiation of this utility class
  }

  /** Returns the host OS bitness, which can differ from the launcher's Java process bitness. */
  public static boolean is64BitOS() {
    return OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
        ? WindowsArchitecture.ARCH.contains("64")
        : System.getProperty("os.arch").contains("64");
  }

  /** Returns the host architecture on Windows; other platforms retain JVM-based detection. */
  public static boolean isArm64OS() {
    return OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
        ? "aarch64".equals(WindowsArchitecture.ARCH)
        : JavaUtils.isArm64();
  }

  private static final class WindowsArchitecture {
    // Cache host information, but never initialize JNA on Linux or macOS.
    private static final String ARCH = query();

    private static String query() {
      try {
        NativeLibrary kernel32 = NativeLibrary.getInstance("kernel32");
        Function isWow64Process2;
        try {
          isWow64Process2 = kernel32.getFunction("IsWow64Process2", Function.ALT_CONVENTION);
        } catch (UnsatisfiedLinkError unavailable) {
          // Before Windows 10 1709, GetNativeSystemInfo handles the x86-on-x64 case.
          // It is not sufficient for modern ARM64 emulation, so prefer IsWow64Process2.
          return queryLegacy(kernel32);
        }
        try (Memory machines = new Memory(4)) {
          machines.clear();
          Object process =
              kernel32
                  .getFunction("GetCurrentProcess", Function.ALT_CONVENTION)
                  .invokePointer(new Object[0]);
          if (isWow64Process2.invokeInt(new Object[] {process, machines, machines.share(2)}) == 0) {
            throw new IllegalStateException("IsWow64Process2 failed: " + Native.getLastError());
          }
          // Two USHORT outputs: process machine (possibly UNKNOWN), then native host machine.
          switch (Short.toUnsignedInt(machines.getShort(2))) {
            case 0x014c:
              return "x86";
            case 0x8664:
              return "amd64";
            case 0xaa64:
              return "aarch64";
            case 0x0200:
              return "ia64";
            default:
              throw new IllegalStateException("Unrecognized native Windows architecture");
          }
        }
      } catch (LinkageError | RuntimeException e) {
        Utils.getLogger().log(Level.WARNING, "Could not determine native Windows architecture", e);
        String wow64 = System.getenv("PROCESSOR_ARCHITEW6432");
        String arch = wow64 != null ? wow64 : System.getProperty("os.arch");
        return "ARM64".equalsIgnoreCase(arch) ? "aarch64" : arch.toLowerCase(java.util.Locale.ROOT);
      }
    }

    private static String queryLegacy(NativeLibrary kernel32) {
      // SYSTEM_INFO is 36 bytes on x86 and 48 on 64-bit Windows. Its first WORD is the
      // processor architecture on both; allocate enough space for either native layout.
      try (Memory info = new Memory(48)) {
        info.clear();
        kernel32
            .getFunction("GetNativeSystemInfo", Function.ALT_CONVENTION)
            .invokeVoid(new Object[] {info});
        switch (Short.toUnsignedInt(info.getShort(0))) {
          case 0:
            return "x86";
          case 9:
            return "amd64";
          case 12:
            return "aarch64";
          case 6:
            return "ia64";
          default:
            throw new IllegalStateException("Unrecognized Windows processor architecture");
        }
      }
    }
  }

  /**
   * Returns the OS version used for metadata ranges, including the Windows build number. Returns
   * null if Windows version detection fails; an unknown version must not satisfy a range. Legacy
   * version regexes should continue to use the unmodified {@code os.version} property.
   */
  public static String getVersionForRange() {
    return OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
        ? WindowsVersion.VERSION
        : System.getProperty("os.version");
  }

  private static final class WindowsVersion {
    // Initialized only on the first Windows range query. Class initialization is thread-safe,
    // and caches both successful detection and failure without loading JNA on other platforms.
    private static final String VERSION = query();

    private static String query() {
      // Allocate the 276-byte RTL_OSVERSIONINFOW structure in native memory. Its layout is:
      //   0: size, 4: major, 8: minor, 12: build, 16: platform ID (five 4-byte DWORDs),
      //   20: service-pack text (128 two-byte WCHARs).
      // These Windows types have fixed widths and there are no pointers in the structure,
      // so the layout is identical on 32-bit and 64-bit Windows. Closing Memory frees the buffer.
      try (Memory info = new Memory(5 * 4 + 128 * 2)) {
        info.clear();
        // Windows requires the size field to identify which version-information structure we use.
        info.setInt(0, (int) info.size());

        // ntdll.dll comes from Windows; JNA supplies only the bridge needed to call its export.
        // ALT_CONVENTION selects stdcall, which is required on 32-bit Windows. On 64-bit Windows,
        // the platform uses a unified calling convention.
        Function rtlGetVersion =
            NativeLibrary.getInstance("ntdll")
                .getFunction("RtlGetVersion", Function.ALT_CONVENTION);

        // Pass the buffer's address: Windows fills the structure in place and returns NTSTATUS.
        int status = rtlGetVersion.invokeInt(new Object[] {info});
        // RtlGetVersion returns STATUS_SUCCESS (zero) on success; only then read the output fields.
        if (status != 0) {
          throw new IllegalStateException(
              "RtlGetVersion failed with NTSTATUS 0x" + Integer.toHexString(status));
        }

        // DWORDs are unsigned, unlike Java ints. Read major/minor/build at the offsets above;
        // unlike Java's os.version property, this includes the Windows build number.
        return Integer.toUnsignedString(info.getInt(4))
            + "."
            + Integer.toUnsignedString(info.getInt(8))
            + "."
            + Integer.toUnsignedString(info.getInt(12));
      } catch (LinkageError | RuntimeException e) {
        // Native loading/linking or the call itself can fail. Keep the version unknown rather
        // than falling back to Java's truncated or compatibility-adjusted os.version value.
        Utils.getLogger()
            .log(Level.WARNING, "Could not determine the Windows version for metadata ranges", e);
        return null;
      }
    }
  }
}
