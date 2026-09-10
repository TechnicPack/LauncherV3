package net.technicpack.launcher.settings.migration;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.io.UserStore;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA256FileVerifier;
import net.technicpack.minecraftcore.install.processor.InstallerArtifactStore;
import net.technicpack.minecraftcore.install.processor.ModernInstallerArtifactResolver;
import net.technicpack.minecraftcore.install.processor.ModernInstallerLock;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.utilslib.CryptoUtils;
import net.technicpack.utilslib.Utils;

/** One-time migration of canonical shared Maven artifacts, not the separate legacy FML cache. */
public final class LibraryCacheMigration implements IMigrator {
  private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

  @Override
  public String getMigrationVersion() {
    return "3";
  }

  @Override
  public String getMigratedVersion() {
    return "4";
  }

  @Override
  public boolean migrate(
      TechnicSettings settings,
      InstalledPackStore packStore,
      LauncherFileSystem fileSystem,
      UserStore users) {
    Migration migration = new Migration();
    try {
      checkInterrupted();
      Path root = fileSystem.getRootDirectory().toRealPath();
      Path cache = directoryRoot(root, "cache");
      if (cache == null) return true;
      Path libraries = directoryRoot(root, "libraries");
      requireDisjoint(cache, libraries == null ? root.resolve("libraries") : libraries);
      try (ModernInstallerLock ignored =
          ModernInstallerLock.acquire(cache, () -> Thread.currentThread().isInterrupted())) {
        checkInterrupted();
        if (!cache.equals(directoryRoot(root, "cache"))) {
          throw new IOException("Cache root changed while acquiring the installer lock");
        }
        libraries = directoryRoot(root, "libraries");
        requireDisjoint(cache, libraries == null ? root.resolve("libraries") : libraries);
        if (libraries == null) {
          Files.createDirectory(
              InstallerArtifactStore.checkedPath(root, root.resolve("libraries")));
          libraries = directoryRoot(root, "libraries");
        }
        requireDisjoint(cache, libraries);
        migration.cache = cache;
        migration.libraries = libraries;
        Files.walkFileTree(cache, migration);
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      migration.failure("cache", interrupted);
    } catch (IOException | SecurityException failure) {
      migration.failure("cache", failure);
    } finally {
      migration.report();
    }
    return migration.failures == 0;
  }

  private static Path directoryRoot(Path root, String name) throws IOException {
    Path path = InstallerArtifactStore.checkedPath(root, root.resolve(name));
    if (attributes(path) == null) return null;
    Path real = path.toRealPath();
    if (!Files.readAttributes(real, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
        .isDirectory()) {
      throw new IOException("Library migration root is not a directory: " + path);
    }
    return real;
  }

  private static void requireDisjoint(Path cache, Path libraries) throws IOException {
    if (cache.startsWith(libraries) || libraries.startsWith(cache)) {
      throw new IOException(
          "Cache and libraries roots must be disjoint: " + cache + ", " + libraries);
    }
  }

  private static BasicFileAttributes attributes(Path path) throws IOException {
    try {
      return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException absent) {
      return null;
    }
  }

  private static void checkInterrupted() throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedIOException("Library cache migration interrupted");
    }
  }

  // Unlike destination parents, source children must never be followed, even within the cache.
  private static Path checkedSource(Path root, Path path) throws IOException {
    Path checked = InstallerArtifactStore.checkedPath(root, path);
    Path child = root;
    for (Path component : root.relativize(checked)) {
      child = child.resolve(component);
      BasicFileAttributes attrs = attributes(child);
      if (attrs == null) break;
      if (attrs.isSymbolicLink()) throw new IOException("Symlink in cached library path: " + child);
    }
    return checked;
  }

  // Inspect the final entry separately: even an escaping/dangling final symlink is a conflict,
  // not a destination to follow. Escaping parent links, however, are retryable path failures.
  private static Path checkedTarget(Path root, Path path) throws IOException {
    Path target = path.toAbsolutePath().normalize();
    if (target.equals(root) || !target.startsWith(root)) {
      throw new IOException("Library target is outside its repository: " + target);
    }
    if (!target.getParent().equals(root)) {
      InstallerArtifactStore.checkedPath(root, target.getParent());
    }
    return target;
  }

  private static boolean sameFile(BasicFileAttributes before, BasicFileAttributes after) {
    return before != null
        && after != null
        && before.isRegularFile()
        && after.isRegularFile()
        && Objects.equals(before.fileKey(), after.fileKey())
        && before.size() == after.size()
        && before.lastModifiedTime().equals(after.lastModifiedTime());
  }

  private static String hash(Path root, Path path, boolean source) throws IOException {
    checkInterrupted();
    Path checked = source ? checkedSource(root, path) : checkedTarget(root, path);
    BasicFileAttributes before = attributes(checked);
    if (before == null || !before.isRegularFile()) {
      throw new IOException("Library is no longer a regular file: " + checked);
    }
    InstallerArtifactStore.checkedPath(root, checked);
    String digest = CryptoUtils.getSHA256(checked);
    checkInterrupted();
    if (digest == null || !SHA256.matcher(digest).matches()) {
      throw new IOException("Unable to hash library: " + checked);
    }
    if (source) checkedSource(root, checked);
    else checkedTarget(root, checked);
    if (!sameFile(before, attributes(checked))) {
      throw new IOException("Library changed while hashing: " + checked);
    }
    return digest;
  }

  /**
   * Deletes only an unchanged source backed by the verified destination. The caller holds the
   * shared installer lock; these checks do not promise safety against hostile filesystem races.
   */
  static boolean deleteVerifiedSource(
      Path cacheRoot,
      Path librariesRoot,
      Path source,
      Path target,
      String expectedSha256,
      BasicFileAttributes originalAttributes)
      throws IOException {
    try {
      if (!hash(librariesRoot, target, false).equals(expectedSha256)) return false;
      BasicFileAttributes destination = attributes(checkedTarget(librariesRoot, target));
      BasicFileAttributes current = attributes(checkedSource(cacheRoot, source));
      if (current == null) return true;
      if (!sameFile(originalAttributes, current)
          || !hash(cacheRoot, source, true).equals(expectedSha256)
          || !sameFile(originalAttributes, attributes(checkedSource(cacheRoot, source)))
          || !sameFile(destination, attributes(checkedTarget(librariesRoot, target)))) {
        return false;
      }
      checkInterrupted();
    } catch (IOException | SecurityException invalid) {
      return false;
    }
    Files.delete(source);
    return true;
  }

  private static final class Migration extends SimpleFileVisitor<Path> {
    private Path cache;
    private Path libraries;
    private final Set<Path> prune = new HashSet<>();
    private int moved;
    private int deduplicated;
    private int conflicts;
    private int failures;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      checkInterrupted();
      if (excluded(cache.relativize(dir))) return FileVisitResult.SKIP_SUBTREE;
      try {
        if (!dir.equals(cache)) checkedSource(cache, dir);
      } catch (IOException | SecurityException failure) {
        failure(cache.relativize(dir).toString(), failure);
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      checkInterrupted();
      MavenCoordinate coordinate =
          attrs.isRegularFile() ? coordinate(cache.relativize(file)) : null;
      if (coordinate != null) {
        try {
          transfer(file, MavenCoordinate.resolve(libraries, coordinate.getPath()));
        } catch (IOException | SecurityException failure) {
          failure(cache.relativize(file).toString(), failure);
          if (Thread.currentThread().isInterrupted()) return FileVisitResult.TERMINATE;
        }
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException failure) throws IOException {
      checkInterrupted();
      Path relative = cache.relativize(file);
      if (!excluded(relative)
          && !(failure instanceof NoSuchFileException
              && Files.notExists(file, LinkOption.NOFOLLOW_LINKS))) {
        failure(relative.toString(), failure);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
      checkInterrupted();
      if (failure != null) failure(cache.relativize(dir).toString(), failure);
      if (!dir.equals(cache) && prune.remove(dir)) {
        try {
          checkedSource(cache, dir);
          Files.delete(dir);
          prune.add(dir.getParent());
        } catch (DirectoryNotEmptyException ignored) {
          // Unrelated files and optional metadata keep their directory.
        } catch (IOException | SecurityException unableToPrune) {
          Utils.getLogger()
              .log(Level.FINE, "Unable to prune migrated cache directory: " + dir, unableToPrune);
        }
      }
      return FileVisitResult.CONTINUE;
    }

    private void transfer(Path source, Path target) throws IOException {
      transfer(source, target, null);
    }

    private void transfer(Path source, Path target, Path payloadTarget) throws IOException {
      BasicFileAttributes original = attributes(checkedSource(cache, source));
      String sha1 = null;
      if (payloadTarget != null) {
        sha1 = ModernInstallerArtifactResolver.readSidecar(source, payloadTarget);
        if (sha1 == null) return;
        checkedTarget(libraries, payloadTarget);
        BasicFileAttributes payload = attributes(payloadTarget);
        if (payload == null || !payload.isRegularFile()) {
          throw new IOException("Published library is no longer regular: " + payloadTarget);
        }
        InstallerArtifactStore.checkedPath(libraries, payloadTarget);
        checkInterrupted();
        boolean valid = new SHA1FileVerifier(sha1).isFileValid(payloadTarget);
        checkInterrupted();
        if (!valid) return;
      }
      String digest = hash(cache, source, true);
      // Bind the parsed checksum to the bytes that staged publication will verify, even if a
      // companion was edited with its original size and timestamp restored before hashing.
      if (sha1 != null
          && !sha1.equals(ModernInstallerArtifactResolver.readSidecar(source, payloadTarget))) {
        throw new IOException("Cached SHA-1 companion changed before transfer: " + source);
      }
      if (!sameFile(original, attributes(checkedSource(cache, source)))) {
        throw new IOException("Cached library changed before transfer: " + source);
      }
      BasicFileAttributes destination = attributes(checkedTarget(libraries, target));
      boolean copied = destination == null;
      if (!copied
          && (!destination.isRegularFile() || !hash(libraries, target, false).equals(digest))) {
        conflicts++;
        Utils.getLogger()
            .warning(
                "Keeping cached library with a different destination: " + cache.relativize(source));
        return;
      }
      if (copied) {
        checkedSource(cache, source);
        InstallerArtifactStore.checkedPath(libraries, checkedTarget(libraries, target));
        try {
          if (!InstallerArtifactStore.copyIfValid(source, target, new SHA256FileVerifier(digest))) {
            throw new IOException("Cached library failed copy verification: " + source);
          }
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          InterruptedIOException failure = new InterruptedIOException("Library copy interrupted");
          failure.initCause(interrupted);
          throw failure;
        }
      }
      if (payloadTarget == null) {
        Path companion = source.resolveSibling(source.getFileName() + ".sha1");
        // The payload's parent was already checked; inspect the companion without following it.
        checkedSource(cache, source);
        BasicFileAttributes sidecar = attributes(companion);
        if (sidecar != null && sidecar.isRegularFile()) {
          transfer(companion, target.resolveSibling(target.getFileName() + ".sha1"), target);
        }
      }
      if (!deleteVerifiedSource(cache, libraries, source, target, digest, original)) {
        throw new IOException("Cached library changed before deletion: " + source);
      }
      prune.add(source.getParent());
      if (copied) moved++;
      else deduplicated++;
    }

    private void failure(String path, Exception cause) {
      failures++;
      Utils.getLogger().log(Level.WARNING, "Unable to migrate cached library: " + path, cause);
    }

    private void report() {
      Utils.getLogger()
          .log(
              conflicts > 0 || failures > 0 ? Level.WARNING : Level.INFO,
              String.format(
                  "Library cache migration: moved=%d, deduplicated=%d, conflicts=%d, failures=%d",
                  moved, deduplicated, conflicts, failures));
    }
  }

  private static boolean excluded(Path relative) {
    if (relative.toString().isEmpty()) return false;
    String first = relative.getName(0).toString();
    return first.equals("fmllibs")
        || first.equals("processor-state")
        || first.startsWith("modern-installer-")
        || first.equals("modern-installer.lock");
  }

  private static MavenCoordinate coordinate(Path relative) {
    int count = relative.getNameCount();
    if (count < 4 || excluded(relative)) return null;
    String filename = relative.getFileName().toString();
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".sha1")
        || lower.endsWith(".md5")
        || lower.endsWith(".sha256")
        || lower.endsWith(".sha512")
        || lower.endsWith(".asc")) return null;
    String artifact = relative.getName(count - 3).toString();
    String version = relative.getName(count - 2).toString();
    String prefix = artifact + "-" + version;
    if (!filename.startsWith(prefix)) return null;
    String suffix = filename.substring(prefix.length());
    String classifier = "";
    String extension;
    if (suffix.startsWith(".")) {
      extension = suffix.substring(1);
    } else if (suffix.startsWith("-")) {
      int dot = suffix.indexOf('.', 1);
      if (dot <= 1) return null;
      classifier = ":" + suffix.substring(1, dot);
      extension = suffix.substring(dot + 1);
    } else {
      return null;
    }
    StringBuilder group = new StringBuilder();
    StringBuilder path = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) path.append('/');
      path.append(relative.getName(i));
      if (i < count - 3) {
        if (i > 0) group.append('.');
        group.append(relative.getName(i));
      }
    }
    try {
      MavenCoordinate coordinate =
          MavenCoordinate.parse(
              group + ":" + artifact + ":" + version + classifier + "@" + extension);
      return coordinate.getPath().equals(path.toString()) ? coordinate : null;
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }
}
