package net.technicpack.utilslib;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import java.util.logging.Level;

public class OSUtils {
  /**
   * Indicates whether the current process is running under WOW64 (Windows 32-bit on Windows
   * 64-bit). This is determined by checking for the presence of the PROCESSOR_ARCHITEW6432
   * environment variable, which is only set when a 32-bit process is running on a 64-bit Windows
   * system.
   */
  private static final boolean IS_WOW64_PROCESS;

  static {
    IS_WOW64_PROCESS =
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS
            && System.getenv("PROCESSOR_ARCHITEW6432") != null;
  }

  private OSUtils() {
    // Prevent instantiation of this utility class
  }

  /**
   * Checks if the current process is running under WOW64 (Windows 32-bit on Windows 64-bit). WOW64
   * is the subsystem that allows 32-bit Windows applications to run on 64-bit Windows.
   *
   * @return true if this is a 32-bit process running on 64-bit Windows, false otherwise
   */
  public static boolean isWow64Process() {
    return IS_WOW64_PROCESS;
  }

  /**
   * Checks if the current operating system is 64-bit. This is determined by checking the system
   * architecture property.
   *
   * <p>NOTE: ARM64 is also considered as 64-bit.
   *
   * @return true if the OS is 64-bit, false otherwise
   */
  public static boolean is64BitOS() {
    if (isWow64Process()) {
      return true;
    }
    String arch = System.getProperty("os.arch");
    return arch.contains("64");
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
