/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.launch.java.source.os;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import net.technicpack.launchercore.launch.java.IVersionSource;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaRuntime;
import net.technicpack.utilslib.Utils;

/** Discovers installed Java homes in both Windows registry views without invoking reg.exe. */
public class WinRegistryJavaSource implements IVersionSource {
  private static final int KEY_WOW64_32KEY = 0x0200;
  private static final int KEY_WOW64_64KEY = 0x0100;
  private static final int KEY_QUERY_VALUE = 0x0001;
  private static final int KEY_ENUMERATE_SUB_KEYS = 0x0008;
  private static final int ERROR_SUCCESS = 0;
  private static final int ERROR_FILE_NOT_FOUND = 2;
  private static final int ERROR_PATH_NOT_FOUND = 3;
  private static final int ERROR_MORE_DATA = 234;
  private static final int ERROR_NO_MORE_ITEMS = 259;
  private static final int REG_SZ = 1;

  @Override
  public void enumerateVersions(JavaVersionRepository repository) {
    Set<String> homes = new LinkedHashSet<>();
    try {
      Registry registry = new Registry();
      // Predefined HKEYs are sign-extended from a signed 32-bit value, even on 64-bit Windows.
      Pointer machine = Pointer.createConstant((long) (int) 0x80000002);
      String[][] vendors = {
        {"Software\\JavaSoft", "JavaHome"},
        {"Software\\AdoptOpenJDK", "Path"},
        {"Software\\Eclipse Foundation", "Path"},
        {"Software\\Eclipse Adoptium", "Path"}
      };
      for (String[] vendor : vendors) {
        WString valueName = new WString(vendor[1]);
        enumerateKey(registry, machine, vendor[0], valueName, KEY_WOW64_32KEY, homes);
        enumerateKey(registry, machine, vendor[0], valueName, KEY_WOW64_64KEY, homes);
      }
      enumerateKey(
          registry,
          machine,
          "Software\\Microsoft\\JDK",
          new WString("Path"),
          KEY_WOW64_64KEY,
          homes);
      // Preserve discovery order when several vendors provide the same Java version.
      WString installationPath = new WString("InstallationPath");
      enumerateKey(
          registry,
          machine,
          "Software\\Azul Systems\\Zulu",
          installationPath,
          KEY_WOW64_32KEY,
          homes);
      enumerateKey(
          registry,
          machine,
          "Software\\Azul Systems\\Zulu",
          installationPath,
          KEY_WOW64_64KEY,
          homes);
      enumerateKey(
          registry,
          machine,
          "Software\\BellSoft\\Liberica",
          installationPath,
          KEY_WOW64_32KEY,
          homes);
      enumerateKey(
          registry,
          machine,
          "Software\\BellSoft\\Liberica",
          installationPath,
          KEY_WOW64_64KEY,
          homes);
    } catch (LinkageError | RuntimeException e) {
      Utils.getLogger()
          .log(Level.WARNING, "Could not enumerate Java installations in the registry", e);
    }
    // Close all registry handles before probing executables. A registry entry is not proof
    // that Java exists, runs successfully, or has the architecture implied by its registry view.
    for (String home : homes) {
      repository.addVersion(new FileBasedJavaRuntime(new File(home, "bin\\javaw.exe")));
    }
  }

  private static void enumerateKey(
      Registry registry,
      Pointer parent,
      String subKey,
      WString valueName,
      int view,
      Set<String> homes) {
    try (Memory result = new Memory(Native.POINTER_SIZE)) {
      result.clear();
      int status =
          registry.open.invokeInt(
              new Object[] {
                parent,
                new WString(subKey),
                0,
                KEY_QUERY_VALUE | KEY_ENUMERATE_SUB_KEYS | view,
                result
              });
      if (status != ERROR_SUCCESS) {
        logError("RegOpenKeyExW", subKey, status);
        return;
      }
      Pointer key = result.getPointer(0);
      try {
        String home = readHome(registry, key, valueName);
        if (home != null && !home.isEmpty()) homes.add(home);

        // A registry key name is at most 255 UTF-16 code units, plus its terminator.
        try (Memory name = new Memory(256 * 2);
            Memory length = new Memory(4)) {
          for (int index = 0; ; index++) {
            length.setInt(0, 256);
            status =
                registry.enumerate.invokeInt(
                    new Object[] {key, index, name, length, null, null, null, null});
            if (status == ERROR_NO_MORE_ITEMS) break;
            if (status != ERROR_SUCCESS) {
              logError("RegEnumKeyExW", subKey, status);
              break;
            }
            // Explicitly retain the selected view when opening each child.
            enumerateKey(registry, key, name.getWideString(0), valueName, view, homes);
          }
        }
      } finally {
        logError("RegCloseKey", subKey, registry.close.invokeInt(new Object[] {key}));
      }
    }
  }

  private static String readHome(Registry registry, Pointer key, WString valueName) {
    try (Memory type = new Memory(4);
        Memory size = new Memory(4)) {
      size.setInt(0, 0);
      int status = registry.query.invokeInt(new Object[] {key, valueName, null, type, null, size});
      if (status != ERROR_SUCCESS) {
        logError("RegQueryValueExW", valueName.toString(), status);
        return null;
      }
      while (type.getInt(0) == REG_SZ) {
        int bytes = size.getInt(0);
        // Windows paths cannot exceed 32,767 UTF-16 code units. Reject malformed values,
        // including odd byte counts, before allocating or interpreting native string data.
        if (bytes <= 0 || bytes > 65536 || (bytes & 1) != 0) return null;
        try (Memory data = new Memory(bytes + 2L)) {
          // RegQueryValueExW does not guarantee a terminator. Leave an extra zero WCHAR.
          data.clear();
          status = registry.query.invokeInt(new Object[] {key, valueName, null, type, data, size});
          if (status == ERROR_MORE_DATA) continue;
          if (status != ERROR_SUCCESS) {
            logError("RegQueryValueExW", valueName.toString(), status);
            return null;
          }
          if (type.getInt(0) != REG_SZ || (size.getInt(0) & 1) != 0) return null;
          return data.getWideString(0);
        }
      }
      return null;
    }
  }

  private static void logError(String operation, String name, int status) {
    if (status != ERROR_SUCCESS
        && status != ERROR_FILE_NOT_FOUND
        && status != ERROR_PATH_NOT_FOUND) {
      Utils.getLogger()
          .warning(operation + " failed for " + name + " (Windows error " + status + ")");
    }
  }

  private static final class Registry {
    private final Function open;
    private final Function enumerate;
    private final Function query;
    private final Function close;

    private Registry() {
      NativeLibrary library = NativeLibrary.getInstance("advapi32");
      open = library.getFunction("RegOpenKeyExW", Function.ALT_CONVENTION);
      enumerate = library.getFunction("RegEnumKeyExW", Function.ALT_CONVENTION);
      query = library.getFunction("RegQueryValueExW", Function.ALT_CONVENTION);
      close = library.getFunction("RegCloseKey", Function.ALT_CONVENTION);
    }
  }
}
