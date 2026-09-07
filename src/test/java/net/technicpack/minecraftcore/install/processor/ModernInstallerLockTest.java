package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernInstallerLockTest {
  @TempDir Path tempDir;

  @Test
  void contendingThreadAcquiresOnlyAfterOwnerCloses() throws Exception {
    Path cache = tempDir.resolve("cache");
    CountDownLatch attempting = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> waiter;
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(cache, () -> false)) {
        waiter =
            executor.submit(
                () -> {
                  try (ModernInstallerLock next =
                      ModernInstallerLock.acquire(
                          cache,
                          () -> {
                            attempting.countDown();
                            return false;
                          })) {
                    return true;
                  }
                });
        assertTrue(attempting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiter.get(250, TimeUnit.MILLISECONDS));
      }
      assertTrue(waiter.get(5, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void cancellingWaiterDoesNotReleaseOwnersOperatingSystemLock() throws Exception {
    Path cache = tempDir.resolve("cache");
    AtomicBoolean cancelled = new AtomicBoolean();
    CountDownLatch attempting = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(cache, () -> false)) {
        Future<?> waiter =
            executor.submit(
                () -> {
                  try (ModernInstallerLock next =
                      ModernInstallerLock.acquire(
                          cache,
                          () -> {
                            attempting.countDown();
                            return cancelled.get();
                          })) {
                    return null;
                  }
                });
        assertTrue(attempting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiter.get(250, TimeUnit.MILLISECONDS));
        cancelled.set(true);
        ExecutionException failure =
            assertThrows(ExecutionException.class, () -> waiter.get(5, TimeUnit.SECONDS));
        assertTrue(failure.getCause() instanceof InterruptedException);
        assertEquals(23, probeLockFromAnotherJvm(cache.resolve("modern-installer.lock")));
      }
      assertEquals(0, probeLockFromAnotherJvm(cache.resolve("modern-installer.lock")));
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(cache, () -> false)) {
        assertEquals(23, probeLockFromAnotherJvm(cache.resolve("modern-installer.lock")));
      }
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void interruptingWaiterAbortsWithoutReleasingOwner() throws Exception {
    Path cache = tempDir.resolve("cache");
    CountDownLatch attempting = new CountDownLatch(1);
    AtomicReference<Thread> waitingThread = new AtomicReference<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(cache, () -> false)) {
        Future<?> waiter =
            executor.submit(
                () -> {
                  waitingThread.set(Thread.currentThread());
                  try (ModernInstallerLock next =
                      ModernInstallerLock.acquire(
                          cache,
                          () -> {
                            attempting.countDown();
                            return false;
                          })) {
                    return null;
                  }
                });
        assertTrue(attempting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiter.get(250, TimeUnit.MILLISECONDS));
        waitingThread.get().interrupt();
        ExecutionException failure =
            assertThrows(ExecutionException.class, () -> waiter.get(5, TimeUnit.SECONDS));
        assertTrue(failure.getCause() instanceof InterruptedException);
        assertEquals(23, probeLockFromAnotherJvm(cache.resolve("modern-installer.lock")));
      }
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static int probeLockFromAnotherJvm(Path lockFile) throws Exception {
    String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    Path java = Paths.get(System.getProperty("java.home"), "bin", executable);
    Path testClasses =
        Paths.get(LockProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Process child =
        new ProcessBuilder(
                java.toString(),
                "-cp",
                testClasses.toString(),
                LockProbe.class.getName(),
                lockFile.toString())
            .inheritIO()
            .start();
    try {
      assertTrue(child.waitFor(5, TimeUnit.SECONDS), "lock probe did not finish");
      return child.exitValue();
    } finally {
      if (child.isAlive()) {
        child.destroyForcibly();
        assertTrue(child.waitFor(5, TimeUnit.SECONDS), "lock probe did not terminate");
      }
    }
  }

  /** Pure JDK child: a separate process observes actual OS ownership, not Java's overlap table. */
  public static final class LockProbe {
    public static void main(String[] args) throws IOException {
      boolean acquired;
      try (FileChannel channel = FileChannel.open(Paths.get(args[0]), StandardOpenOption.WRITE);
          FileLock lock = channel.tryLock()) {
        acquired = lock != null;
      }
      System.exit(acquired ? 0 : 23);
    }
  }
}
