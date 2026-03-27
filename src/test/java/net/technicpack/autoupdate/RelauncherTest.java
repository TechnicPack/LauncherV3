package net.technicpack.autoupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RelauncherTest {
  private static final int EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS = 20;

  @Test
  void replaceLauncherPackageRetriesWhenWindowsTemporarilyLocksTarget() throws Exception {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();
    List<Long> pauses = new ArrayList<>();

    Relauncher.replaceLauncherPackage(
        currentPath,
        targetPath,
        true,
        (source, target) -> {
          if (attempts.incrementAndGet() < 3) {
            throw new AccessDeniedException(target.toString());
          }
        },
        pauses::add);

    assertEquals(3, attempts.get());
    assertEquals(List.of(250L, 250L), pauses);
  }

  @Test
  void replaceLauncherPackageDoesNotRetryWhenFailureDoesNotTargetDestination() {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();

    FileSystemException error =
        assertThrows(
            FileSystemException.class,
            () ->
                Relauncher.replaceLauncherPackage(
                    currentPath,
                    targetPath,
                    true,
                    (source, target) -> {
                      attempts.incrementAndGet();
                      throw new FileSystemException(source.toString());
                    },
                    millis -> {}));

    assertEquals(1, attempts.get());
    assertEquals(currentPath.toString(), error.getFile());
  }

  @Test
  void replaceLauncherPackageStopsAfterConfiguredRetryLimit() {
    Path currentPath = Path.of("temp.exe");
    Path targetPath = Path.of("TechnicLauncher.exe");
    AtomicInteger attempts = new AtomicInteger();
    List<Long> pauses = new ArrayList<>();

    AccessDeniedException error =
        assertThrows(
            AccessDeniedException.class,
            () ->
                Relauncher.replaceLauncherPackage(
                    currentPath,
                    targetPath,
                    true,
                    (source, target) -> {
                      attempts.incrementAndGet();
                      throw new AccessDeniedException(target.toString());
                    },
                    pauses::add));

    assertEquals(targetPath.toString(), error.getFile());
    assertEquals(EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS, attempts.get());
    assertEquals(EXPECTED_WINDOWS_PACKAGE_REPLACE_ATTEMPTS - 1, pauses.size());
  }
}
