package net.technicpack.minecraftcore.install.processor;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;

/** Standalone child entry point: depends only on the JDK and JNA, never installer classes. */
public final class ProcessorCompressorProbe {
  private ProcessorCompressorProbe() {}

  public static void main(String[] arguments) {
    try {
      if (!"Linux".equals(System.getProperty("os.name"))) {
        unknown("native compressor inspection is only supported on Linux/glibc");
        return;
      }
      // Force this JVM to load its compression implementation before inspecting mapped libraries.
      Deflater deflater = new Deflater();
      deflater.end();
      Set<String> paths = new LinkedHashSet<>();
      try (BufferedReader maps =
          Files.newBufferedReader(Paths.get("/proc/self/maps"), StandardCharsets.UTF_8)) {
        String line;
        while ((line = maps.readLine()) != null) {
          String[] columns = line.split("\\s+", 6);
          if (columns.length == 6 && columns[5].endsWith("/libzip.so")) {
            paths.add(columns[5]);
          }
        }
      }
      // Ambiguous mappings or statically bundled compression must never produce a positive result.
      if (paths.size() != 1) {
        unknown("could not identify one mapped JVM libzip.so");
        return;
      }
      try (NativeLibrary process = NativeLibrary.getProcess()) {
        // RTLD_NOLOAD has platform-specific values. Do not use the glibc flags on another libc.
        process.getFunction("gnu_get_libc_version");
        Function open = process.getFunction("dlopen");
        Function symbol = process.getFunction("dlsym");
        Function close = process.getFunction("dlclose");
        // Call dlopen directly: NativeLibrary's library-name search/fallback must not pick another
        // library. RTLD_NOLOAD | RTLD_LAZY = 5 opens only the exact, already-loaded object.
        Pointer zip = open.invokePointer(new Object[] {paths.iterator().next(), 5});
        if (zip == null) {
          unknown("mapped libzip.so could not be opened without loading it");
          return;
        }
        String version;
        try {
          Pointer address = symbol.invokePointer(new Object[] {zip, "zlibVersion"});
          if (address == null) {
            unknown("libzip.so does not expose its compressor version");
            return;
          }
          version = Function.getFunction(address).invokeString(new Object[0], false);
        } finally {
          close.invokeInt(new Object[] {zip});
        }
        if (version == null || version.isEmpty()) {
          unknown("compressor returned no version");
        } else {
          System.err.println("Selected JVM compressor version: " + version);
          System.out.println(
              version.toLowerCase(Locale.ROOT).contains("zlib-ng")
                  ? "TECHNIC_PROCESSOR_COMPRESSOR=ZLIB_NG"
                  : "TECHNIC_PROCESSOR_COMPRESSOR=OTHER");
        }
      }
    } catch (Exception | LinkageError failure) {
      unknown(failure.toString());
    }
  }

  private static void unknown(String reason) {
    System.err.println("Selected JVM compressor unknown: " + reason);
    System.out.println("TECHNIC_PROCESSOR_COMPRESSOR=UNKNOWN");
  }
}
