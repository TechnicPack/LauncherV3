package net.technicpack.launchercore.launch.java.version;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.launchercore.exception.JavaRuntimeException;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.utilslib.OperatingSystem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileBasedJavaRuntimeTest {
  @TempDir Path tempDir;

  @Test
  void missingExecutableIsRejectedWithItsPathInsteadOfBeingSelected() {
    Path executable = tempDir.resolve("missing-java");
    FileBasedJavaRuntime runtime = new FileBasedJavaRuntime(executable);

    assertFalse(new JavaVersionRepository().addVersion(runtime));
    JavaRuntimeException failure = assertThrows(JavaRuntimeException.class, runtime::validate);
    assertTrue(failure.getMessage().contains(executable.toString()));
  }

  @Test
  void blankArchitectureIsRejectedAndProbeDiagnosticsArePreserved() throws Exception {
    Assumptions.assumeTrue(OperatingSystem.getOperatingSystem() == OperatingSystem.LINUX);
    Path executable = tempDir.resolve("java");
    Files.write(
        executable,
        ("#!/bin/sh\n"
                + "echo 'java.version = 17.0.1'\n"
                + "echo 'java.vendor = Test runtime'\n"
                + "echo 'os.arch = '\n"
                + "echo 'Architecture detection failed' >&2\n")
            .getBytes(StandardCharsets.UTF_8));
    assertTrue(executable.toFile().setExecutable(true));
    FileBasedJavaRuntime runtime = new FileBasedJavaRuntime(executable);

    assertFalse(new JavaVersionRepository().addVersion(runtime));
    JavaRuntimeException failure = assertThrows(JavaRuntimeException.class, runtime::validate);
    assertTrue(failure.getMessage().contains("Architecture detection failed"));
  }
}
