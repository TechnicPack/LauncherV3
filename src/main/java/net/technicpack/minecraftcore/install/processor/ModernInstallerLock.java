package net.technicpack.minecraftcore.install.processor;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Serializes shared repository writes and complete processor runs across threads and launchers.
 *
 * <p>Acquire once at the execution boundary and pass ownership inward. This lock is not reentrant;
 * helpers operating under it must never acquire it recursively. Closing the channel releases its
 * file lock, but deliberately leaves the shared lock file in place.
 */
public final class ModernInstallerLock implements AutoCloseable {
  // Closing another channel to this file can release the owner's OS lock on some platforms.
  // Reserve its real path before opening a channel, and retain no entries after owners close.
  private static final Set<Path> OPEN_LOCK_FILES = new HashSet<>();

  private final FileChannel channel;
  private final Path lockIdentity;
  private boolean closed;

  private ModernInstallerLock(FileChannel channel, Path lockIdentity) {
    this.channel = channel;
    this.lockIdentity = lockIdentity;
  }

  public static ModernInstallerLock acquire(Path cacheDirectory, BooleanSupplier cancelled)
      throws IOException, InterruptedException {
    Objects.requireNonNull(cancelled, "cancelled");
    checkCancelled(cancelled);
    Path cache = cacheDirectory.toAbsolutePath().normalize();
    Files.createDirectories(cache);
    Path lockPath =
        InstallerArtifactStore.checkedPath(cache, cache.resolve("modern-installer.lock"));
    Path lockIdentity;
    try {
      lockIdentity = lockPath.toRealPath();
    } catch (NoSuchFileException missing) {
      lockIdentity = cache.toRealPath().resolve(lockPath.getFileName());
    }
    while (true) {
      checkCancelled(cancelled);
      synchronized (OPEN_LOCK_FILES) {
        if (OPEN_LOCK_FILES.add(lockIdentity)) {
          break;
        }
      }
      Thread.sleep(100);
    }

    FileChannel channel = null;
    try {
      channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      while (true) {
        checkCancelled(cancelled);
        FileLock lock = null;
        try {
          lock = channel.tryLock();
        } catch (OverlappingFileLockException ignored) {
          // Treat locks acquired outside this helper in the same JVM as busy, too.
        }
        if (lock != null) {
          checkCancelled(cancelled);
          return new ModernInstallerLock(channel, lockIdentity);
        }
        Thread.sleep(100);
      }
    } catch (IOException | InterruptedException | RuntimeException | Error failure) {
      try {
        if (channel != null) {
          channel.close();
        }
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      } finally {
        synchronized (OPEN_LOCK_FILES) {
          OPEN_LOCK_FILES.remove(lockIdentity);
        }
      }
      throw failure;
    }
  }

  private static void checkCancelled(BooleanSupplier cancelled) throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new InterruptedException("Modern installer lock acquisition cancelled");
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    try {
      channel.close();
    } finally {
      closed = true;
      synchronized (OPEN_LOCK_FILES) {
        OPEN_LOCK_FILES.remove(lockIdentity);
      }
    }
  }
}
