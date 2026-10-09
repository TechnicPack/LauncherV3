package net.technicpack.launcher.io;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.LINUX)
class LinuxLaunchScriptTest {
  @TempDir Path directory;

  @Test
  void anotherJarCannotRetargetExistingWrapper() throws Exception {
    Path installed = Files.createFile(directory.resolve("installed.jar"));
    Path development = Files.createFile(directory.resolve("development.jar"));
    LinuxLaunchScript.install(directory, installed);
    Path script = directory.resolve("technic-launcher");
    byte[] original = Files.readAllBytes(script);
    FileTime sentinel = FileTime.fromMillis(1000000);
    Files.setLastModifiedTime(script, sentinel);
    LinuxLaunchScript.install(directory, development);
    assertArrayEquals(original, Files.readAllBytes(script));
    assertEquals(sentinel, Files.getLastModifiedTime(script));
    assertTrue(Files.isExecutable(script));
  }

  @Test
  void preservesUserEditedWrapperAndItsPermissions() throws Exception {
    Path jar = Files.createFile(directory.resolve("launcher.jar"));
    Path script = directory.resolve("technic-launcher");
    byte[] custom = "#!/bin/sh\nexec custom-launcher\n".getBytes(StandardCharsets.UTF_8);
    Files.write(script, custom);
    java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions =
        Files.getPosixFilePermissions(script);
    LinuxLaunchScript.install(directory, jar);
    assertArrayEquals(custom, Files.readAllBytes(script));
    assertEquals(permissions, Files.getPosixFilePermissions(script));
  }
}
