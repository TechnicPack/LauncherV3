package net.technicpack.launcher.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinuxDesktopEntryTest {
  @TempDir Path directory;

  @Test
  void acceptsOnlyAbsoluteXdgDataDirectory() {
    Path home = directory.resolve("home");
    Path data = directory.resolve("custom data");
    assertEquals(data, LinuxDesktopEntry.dataDirectory(data.toString(), home));
    for (String value : new String[] {null, "", "relative/data"}) {
      assertEquals(home.resolve(".local/share"), LinuxDesktopEntry.dataDirectory(value, home));
    }
  }

  @Test
  void preservesUserShortcutAndUnchangedFiles() throws Exception {
    Path applications = Files.createDirectories(directory.resolve("applications"));
    Path manual = applications.resolve("Technic Launcher.desktop");
    byte[] custom = "user shortcut".getBytes(StandardCharsets.UTF_8);
    Files.write(manual, custom);
    byte[] icon = {1, 2, 3};
    Path wrapper = directory.resolve("first/technic-launcher");
    LinuxDesktopEntry.install(directory, wrapper, new ByteArrayInputStream(icon));
    Path entry = applications.resolve("net.technicpack.launcher.desktop");
    Path installedIcon = directory.resolve("icons/net.technicpack.launcher.png");
    FileTime sentinel = FileTime.fromMillis(1000000);
    Files.setLastModifiedTime(entry, sentinel);
    Files.setLastModifiedTime(installedIcon, sentinel);
    LinuxDesktopEntry.install(directory, wrapper, new ByteArrayInputStream(icon));
    assertEquals(sentinel, Files.getLastModifiedTime(entry));
    assertEquals(sentinel, Files.getLastModifiedTime(installedIcon));
    assertArrayEquals(icon, Files.readAllBytes(installedIcon));
    assertArrayEquals(custom, Files.readAllBytes(manual));
  }
}
