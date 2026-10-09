package net.technicpack.launcher.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;

/** Installs the standard Linux launch command without changing portable installations. */
public final class LinuxLaunchScript {
  private LinuxLaunchScript() {}

  public static void install(Path directory, Path launcher) throws IOException {
    Path jar = launcher.toAbsolutePath().normalize();
    if (!Files.isRegularFile(jar) || !jar.toString().endsWith(".jar")) {
      return;
    }
    Path script = directory.resolve("technic-launcher");
    String quotedJar = "'" + jar.toString().replace("'", "'\"'\"'") + "'";
    byte[] content =
        ("#!/bin/sh\n"
                + "exec java -Djava.net.preferIPv4Stack=true -Dawt.useSystemAAFontSettings=lcd "
                + "-Dswing.aatext=true -jar "
                + quotedJar
                + " \"$@\"\n")
            .getBytes(StandardCharsets.UTF_8);
    if (!Files.exists(script) || !Arrays.equals(Files.readAllBytes(script), content)) {
      Files.write(script, content);
    }
    Files.setPosixFilePermissions(
        script,
        EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE));
  }
}
