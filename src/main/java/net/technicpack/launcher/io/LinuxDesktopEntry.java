package net.technicpack.launcher.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Per-user desktop metadata for the standard Linux installation. */
public final class LinuxDesktopEntry {
  private LinuxDesktopEntry() {}

  public static Path dataDirectory(String xdgDataHome, Path home) {
    if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
      Path path = Paths.get(xdgDataHome);
      if (path.isAbsolute()) {
        return path;
      }
    }
    return home.resolve(".local/share");
  }

  public static void install(Path dataDirectory, Path wrapper, InputStream icon)
      throws IOException {
    if (icon == null) {
      throw new IOException("Bundled launcher icon is missing");
    }
    Path iconPath = dataDirectory.resolve("icons/net.technicpack.launcher.png").toAbsolutePath();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int count;
    while ((count = icon.read(buffer)) != -1) {
      bytes.write(buffer, 0, count);
    }
    writeIfChanged(iconPath, bytes.toByteArray());
    // Keep the executable token fixed: GLib checks it before expanding escaped percent signs.
    String entry =
        "[Desktop Entry]\n"
            + "Type=Application\n"
            + "Name=Technic Launcher\n"
            + "Comment=Install and play Minecraft modpacks\n"
            + "Exec=env "
            + execArgument(wrapper.toAbsolutePath().toString())
            + "\n"
            + "Icon="
            + escapeValue(iconPath.toString())
            + "\n"
            + "Terminal=false\n"
            + "StartupNotify=true\n"
            + "Categories=Game;\n"
            + "StartupWMClass=net-technicpack-launcher-LauncherMain\n";
    writeIfChanged(
        dataDirectory.resolve("applications/net.technicpack.launcher.desktop"),
        entry.getBytes(StandardCharsets.UTF_8));
  }

  private static String execArgument(String value) {
    String quoted =
        value
            .replace("%", "%%")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`");
    return escapeValue("\"" + quoted + "\"");
  }

  private static String escapeValue(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static void writeIfChanged(Path path, byte[] content) throws IOException {
    Files.createDirectories(path.getParent());
    if (!Files.exists(path) || !Arrays.equals(Files.readAllBytes(path), content)) {
      Files.write(path, content);
    }
  }
}
