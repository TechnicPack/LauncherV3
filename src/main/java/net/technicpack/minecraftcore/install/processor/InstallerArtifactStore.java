package net.technicpack.minecraftcore.install.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;

/**
 * Verified, staged publication for installer artifacts.
 *
 * <p>The caller must hold {@link ModernInstallerLock} for every write, including staging, and must
 * check repository/output paths with {@link #checkedPath(Path, Path)} before accessing them. These
 * helpers never acquire the lock themselves: recursive acquisition would deadlock. A staging file
 * belongs to its creator, which must remove it on failure; published targets and source files are
 * never cleanup candidates.
 */
public final class InstallerArtifactStore {
  private InstallerArtifactStore() {}

  public static boolean isValid(Path path, IFileVerifier verifier) {
    Objects.requireNonNull(verifier, "verifier");
    return Files.isRegularFile(path) && verifier.isFileValid(path);
  }

  /**
   * Returns false for a missing or invalid source. A source that changes during copying fails with
   * an IOException rather than replacing an existing target with unverified bytes.
   */
  public static boolean copyIfValid(Path source, Path target, IFileVerifier verifier)
      throws IOException, InterruptedException {
    checkInterrupted();
    boolean validSource = isValid(source, verifier);
    checkInterrupted();
    if (!validSource) {
      return false;
    }

    Path staged = stage(target);
    try {
      try (InputStream input = Files.newInputStream(source);
          OutputStream output = Files.newOutputStream(staged)) {
        byte[] buffer = new byte[8192];
        while (true) {
          checkInterrupted();
          int count = input.read(buffer);
          if (count == -1) {
            break;
          }
          checkInterrupted();
          output.write(buffer, 0, count);
        }
      }
      checkInterrupted();
      boolean validCopy = isValid(staged, verifier);
      checkInterrupted();
      if (!validCopy) {
        throw new IOException("Copied artifact failed verification: " + source + " -> " + target);
      }
      publish(staged, target);
      return true;
    } catch (IOException | InterruptedException | RuntimeException | Error failure) {
      try {
        Files.deleteIfExists(staged);
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  /** Creates an owned, unique sibling without opening or truncating the published target. */
  public static Path stage(Path target) throws IOException {
    Path normalized = target.toAbsolutePath().normalize();
    Path parent = normalized.getParent();
    if (parent == null) {
      throw new IOException("Artifact target must have a parent directory: " + target);
    }
    Files.createDirectories(parent);
    return Files.createTempFile(parent, ".installer-", ".tmp");
  }

  /**
   * Moves a verified sibling over the target. The same-filesystem replacement fallback is safe
   * because the caller holds the shared writer lock; it never streams bytes into a published file.
   * The caller retains ownership of the staging file if publication fails.
   */
  public static void publish(Path staged, Path target) throws IOException {
    Path source = staged.toAbsolutePath().normalize();
    Path destination = target.toAbsolutePath().normalize();
    if (source.equals(destination)
        || source.getParent() == null
        || !source.getParent().equals(destination.getParent())
        || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Artifact staging file must be a regular, distinct sibling: " + staged);
    }
    try {
      Files.move(
          source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException ignored) {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Resolves a target strictly below an existing root, allowing the root itself to be a symlink.
   * Relative targets are relative to the root. Every existing child symlink must resolve within the
   * real root, including the final target; dangling links fail closed. This is a filesystem
   * preflight, not protection against an external process replacing path components after the
   * check.
   */
  public static Path checkedPath(Path root, Path target) throws IOException {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path normalizedTarget =
        (target.isAbsolute() ? target : normalizedRoot.resolve(target))
            .toAbsolutePath()
            .normalize();
    if (normalizedTarget.equals(normalizedRoot) || !normalizedTarget.startsWith(normalizedRoot)) {
      throw new IOException(
          "Installer path must be strictly below " + normalizedRoot + ": " + target);
    }

    Path realRoot = normalizedRoot.toRealPath();
    Path child = normalizedRoot;
    for (Path component : normalizedRoot.relativize(normalizedTarget)) {
      child = child.resolve(component);
      BasicFileAttributes attributes;
      try {
        attributes =
            Files.readAttributes(child, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException missing) {
        // No deeper component can exist once an ordinary path component is absent.
        break;
      }
      if (attributes.isSymbolicLink() && !child.toRealPath().startsWith(realRoot)) {
        throw new IOException("Installer path escapes through a symlink: " + child);
      }
    }
    return normalizedTarget;
  }

  private static void checkInterrupted() throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Artifact copy interrupted");
    }
  }
}
