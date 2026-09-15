package net.technicpack.utilslib;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

/** Fresh, advisory host-memory estimates, independent of the configured Java heap ceiling. */
public final class MemoryPressure {
  // An advisory allowance for non-heap allocations and system activity, not an exact JVM budget.
  public static final long HEADROOM_MB = 1024;

  private MemoryPressure() {}

  /**
   * Returns an estimate in MiB, or -1 when unavailable. Never substitute total RAM for headroom.
   */
  public static long getAvailableMemoryMb() {
    try {
      switch (OperatingSystem.getOperatingSystem()) {
        case WINDOWS:
          java.lang.management.OperatingSystemMXBean bean =
              ManagementFactory.getOperatingSystemMXBean();
          if (bean instanceof com.sun.management.OperatingSystemMXBean) {
            // On Windows OpenJDK uses GlobalMemoryStatusEx.ullAvailPhys, including standby pages.
            long bytes =
                ((com.sun.management.OperatingSystemMXBean) bean).getFreePhysicalMemorySize();
            return bytes < 0 ? -1 : bytes / (1024 * 1024);
          }
          return -1;
        case LINUX:
          // MemFree excludes reclaimable cache and substantially understates available RAM.
          try (BufferedReader reader =
              Files.newBufferedReader(Paths.get("/proc/meminfo"), StandardCharsets.US_ASCII)) {
            return readLinuxAvailableMemory(reader);
          }
        case OSX:
          return MacMemory.availableMb();
        default:
          return -1;
      }
    } catch (IOException | LinkageError | RuntimeException | InternalError e) {
      Utils.getLogger().log(Level.WARNING, "Could not estimate available memory before launch", e);
      return -1;
    }
  }

  public static boolean shouldWarn(long heapMb, long availableMb) {
    return heapMb > 0 && availableMb >= 0 && heapMb > availableMb - HEADROOM_MB;
  }

  static long readLinuxAvailableMemory(BufferedReader reader) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      if (!line.startsWith("MemAvailable:")) continue;
      String value = line.substring("MemAvailable:".length()).trim();
      if (!value.endsWith("kB")) return -1;
      try {
        long kib = Long.parseLong(value.substring(0, value.length() - 2).trim());
        return kib < 0 ? -1 : kib / 1024;
      } catch (NumberFormatException invalid) {
        return -1;
      }
    }
    return -1;
  }

  private static final class MacMemory {
    private static final NativeLibrary SYSTEM = NativeLibrary.getInstance("System");
    private static final Function HOST_SELF = SYSTEM.getFunction("mach_host_self");
    private static final Function HOST_STATISTICS = SYSTEM.getFunction("host_statistics");
    private static final Function HOST_PAGE_SIZE = SYSTEM.getFunction("host_page_size");
    private static final Function DEALLOCATE = SYSTEM.getFunction("mach_port_deallocate");

    private static long availableMb() {
      int host = HOST_SELF.invokeInt(new Object[0]);
      if (host == 0) throw new IllegalStateException("mach_host_self returned no host port");
      try (com.sun.jna.Memory stats = new com.sun.jna.Memory(15 * 4);
          com.sun.jna.Memory count = new com.sun.jna.Memory(4);
          com.sun.jna.Memory pageSize = new com.sun.jna.Memory(Native.SIZE_T_SIZE)) {
        // HOST_VM_INFO uses 15 fixed-width natural_t fields on both Intel and Apple Silicon.
        // free_count is at offset 0, inactive_count at 8. Speculative pages are already free;
        // purgeable pages overlap other queues, so neither should be added a second time.
        stats.clear();
        count.setInt(0, 15);
        pageSize.clear();
        int status = HOST_STATISTICS.invokeInt(new Object[] {host, 2, stats, count});
        if (status != 0 || count.getInt(0) < 3) {
          throw new IllegalStateException("host_statistics failed: " + status);
        }
        status = HOST_PAGE_SIZE.invokeInt(new Object[] {host, pageSize});
        if (status != 0) throw new IllegalStateException("host_page_size failed: " + status);
        long bytesPerPage =
            Native.SIZE_T_SIZE == 8
                ? pageSize.getLong(0)
                : Integer.toUnsignedLong(pageSize.getInt(0));
        if (bytesPerPage <= 0) throw new IllegalStateException("Invalid Mach page size");
        long pages =
            Integer.toUnsignedLong(stats.getInt(0)) + Integer.toUnsignedLong(stats.getInt(8));
        return Math.multiplyExact(pages, bytesPerPage) / (1024 * 1024);
      } finally {
        // mach_host_self acquires a send right. Release it after every sample, including failures.
        int task = SYSTEM.getGlobalVariableAddress("mach_task_self_").getInt(0);
        int status = DEALLOCATE.invokeInt(new Object[] {task, host});
        if (status != 0) Utils.getLogger().warning("mach_port_deallocate failed: " + status);
      }
    }
  }
}
