package net.technicpack.minecraftcore.install.processor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;

/**
 * Successful-run receipts for outputless processors whose file arguments can all be tracked.
 *
 * <p>Every operation, including reads, requires the caller's ModernInstallerLock. Receipts are
 * observations, not authoritative artifact checksums or permission to remove referenced files.
 */
final class ProcessorCache {
  private static final int MAGIC = 0x50524348;
  private static final int VERSION = 1;
  private static final int MAX_FILES = 256;
  private static final int MAX_LABEL_BYTES = 2048;
  private static final long MAX_RECEIPT_BYTES = 1024 * 1024;

  private final Path root;
  private final Path libraries;
  private final Path cache;
  private final Path installer;
  private final Path vanillaJar;
  private final IJavaRuntime runtime;
  private final BooleanSupplier cancelled;
  private Context context;
  private boolean contextAttempted;
  private byte[] hashBuffer;

  ProcessorCache(
      Path root, Path installer, Path vanillaJar, IJavaRuntime runtime, BooleanSupplier cancelled) {
    this.root = root.toAbsolutePath().normalize();
    this.libraries = this.root.resolve("libraries");
    this.cache = this.root.resolve("cache");
    this.installer = installer.toAbsolutePath().normalize();
    this.vanillaJar = vanillaJar.toAbsolutePath().normalize();
    this.runtime = runtime;
    this.cancelled = cancelled;
  }

  void afterExecution() {
    // A child can change shared inputs without changing their size or timestamps.
    context = null;
    contextAttempted = false;
  }

  Entry prepare(
      ModernInstallerProfile profile,
      ModernInstallerProfile.Processor processor,
      int index,
      Map<String, String> resolvedData,
      List<String> resolvedArguments,
      List<Path> verifiedClasspath)
      throws IOException, InterruptedException {
    checkCancelled();
    if (!processor.getOutputs().isEmpty()
        || processor.getArgs().size() != resolvedArguments.size()
        || verifiedClasspath.isEmpty()
        || index < 0) return null;
    try {
      InstallerArtifactStore.checkedPath(root, libraries);
      InstallerArtifactStore.checkedPath(root, cache);
      Fingerprint recipe = new Fingerprint();
      recipe.number(VERSION);
      profileIdentity(recipe, profile);
      recipe.number(index);
      processorIdentity(recipe, processor);
      Map<String, TrackedFile> tracked = new TreeMap<>();
      boolean hasMavenFile = false;
      for (int argumentIndex = 0; argumentIndex < resolvedArguments.size(); argumentIndex++) {
        checkCancelled();
        String raw = processor.getArgs().get(argumentIndex);
        String resolved = resolvedArguments.get(argumentIndex);
        if (raw == null || resolved == null || hasQuoting(raw)) return null;
        recipe.text(raw);
        TrackedFile file = null;
        if (wholeCoordinate(raw)) {
          file = mavenFile(raw, resolved);
          if (file == null) return null;
        } else if (wholeToken(raw)) {
          String token = raw.substring(1, raw.length() - 1);
          if (!resolved.equals(resolvedData.get(token))) return null;
          // These names take precedence over identically named profile data in the resolver.
          if ("ROOT".equals(token) || "LIBRARY_DIR".equals(token)) return null;
          if ("INSTALLER".equals(token)) {
            if (!resolved.equals(installer.toString())) return null;
          } else if ("MINECRAFT_JAR".equals(token)) {
            if (!resolved.equals(vanillaJar.toString())) return null;
          } else if ("SIDE".equals(token)) {
            if (!"client".equals(resolved)) return null;
          } else if ("MINECRAFT_VERSION".equals(token)) {
            if (!resolved.equals(profile.getMinecraft())) return null;
          } else {
            ModernInstallerProfile.DataValue data = profile.getData().get(token);
            if (data == null || data.getClient() == null) return null;
            String source = data.getClient();
            if (wholeCoordinate(source) && !hasQuoting(source)) {
              file = mavenFile(source, resolved);
              if (file == null) return null;
            } else if (source.length() >= 2 && source.startsWith("'") && source.endsWith("'")) {
              if (!resolved.equals(source.substring(1, source.length() - 1)) || !scalar(resolved))
                return null;
            } else {
              file = resourceFile(token, source, resolved);
              if (file == null) return null;
            }
          }
        } else if (!raw.equals(resolved) || !scalar(raw)) {
          // Do not parse compound substitutions or guess whether a literal is a file argument.
          return null;
        }
        if (file == null) {
          recipe.text("scalar");
          recipe.text(resolved);
        } else {
          recipe.text("file");
          recipe.text(file.label);
          tracked.put(file.label, file);
          hasMavenFile |= file.maven;
        }
      }
      if (!hasMavenFile || tracked.size() > MAX_FILES) return null;
      Context fixed = context();
      if (fixed == null || !fixed.unchanged()) return null;
      recipe.bytes(fixed.identity);
      List<TrackedFile> fixedFiles = new ArrayList<>();
      recipe.number(verifiedClasspath.size());
      for (Path path : verifiedClasspath) {
        checkCancelled();
        Path checked = InstallerArtifactStore.checkedPath(libraries, path);
        TrackedFile file = new TrackedFile("classpath:" + checked, checked, libraries, false);
        file.before = snapshot(file);
        if (file.before == null) return null;
        fixedFiles.add(file);
        recipe.text(file.label);
        recipe.bytes(file.before.hash);
      }
      recipe.number(tracked.size());
      for (TrackedFile file : tracked.values()) {
        if (file.labelBytes.length > MAX_LABEL_BYTES) return null;
        file.before = snapshot(file);
        if (!file.maven && file.before == null) return null;
        recipe.text(file.label);
        // Resources are inputs, never outputs. Their extracted temporary path is not identity.
        if (!file.maven) recipe.bytes(file.before.hash);
      }
      Fingerprint slot = new Fingerprint();
      slot.text(profile.getMinecraft());
      slot.text(profile.getVersion());
      slot.number(index);
      Path receipt =
          checkedReceipt(
              cache
                  .resolve("processor-state")
                  .resolve(
                      org.apache.commons.codec.binary.Hex.encodeHexString(slot.finish()) + ".bin"));
      return new Entry(
          receipt, recipe.finish(), new ArrayList<>(tracked.values()), fixedFiles, fixed);
    } catch (IOException | IllegalArgumentException unavailable) {
      // An optional cache must not make previously executable recipes depend on extra files.
      checkCancelled();
      return null;
    }
  }

  private Path checkedReceipt(Path receipt) throws IOException {
    InstallerArtifactStore.checkedPath(root, cache);
    Path checked = InstallerArtifactStore.checkedPath(cache, receipt);
    try {
      BasicFileAttributes directory =
          Files.readAttributes(
              checked.getParent(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!directory.isDirectory()) {
        throw new IOException("Processor state directory is not an ordinary directory");
      }
    } catch (NoSuchFileException missing) {
      // stage() creates this owned directory only when publishing a successful run.
    }
    return checked;
  }

  private TrackedFile mavenFile(String coordinate, String resolved) throws IOException {
    MavenCoordinate parsed =
        MavenCoordinate.parse(coordinate.substring(1, coordinate.length() - 1));
    Path path =
        InstallerArtifactStore.checkedPath(
            libraries, MavenCoordinate.resolve(libraries, parsed.getPath()));
    if (!path.toString().equals(resolved)) return null;
    return new TrackedFile("maven:" + parsed.getPath(), path, libraries, true);
  }

  private TrackedFile resourceFile(String token, String source, String resolved)
      throws IOException {
    if (hasQuoting(source) || source.indexOf('{') >= 0 || source.indexOf('[') >= 0) return null;
    String member = source.startsWith("/") ? source.substring(1) : source;
    Path relative = cache.relativize(MavenCoordinate.resolve(cache, member));
    Path path = InstallerArtifactStore.checkedPath(cache, Paths.get(resolved));
    if (!path.endsWith(relative)
        || path.getNameCount() <= cache.getNameCount() + relative.getNameCount()
        || path.startsWith(cache.resolve("processor-state"))) return null;
    return new TrackedFile("resource:" + token, path, cache, false);
  }

  private static boolean wholeCoordinate(String value) {
    return value.length() > 2
        && value.charAt(0) == '['
        && value.charAt(value.length() - 1) == ']'
        && value.indexOf('[', 1) < 0
        && value.indexOf(']') == value.length() - 1
        && value.indexOf('{') < 0
        && value.indexOf('}') < 0;
  }

  private static boolean wholeToken(String value) {
    if (value.length() <= 2 || !value.startsWith("{") || !value.endsWith("}")) return false;
    for (int index = 1; index < value.length() - 1; index++) {
      char character = value.charAt(index);
      if (!asciiLetterOrDigit(character) && character != '_') return false;
    }
    return true;
  }

  private static boolean hasQuoting(String value) {
    return value.indexOf('\\') >= 0 || value.indexOf('\'') >= 0 || value.indexOf('"') >= 0;
  }

  private static boolean scalar(String value) {
    if (value.isEmpty()) return false;
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (!asciiLetterOrDigit(character) && character != '_' && character != '-') return false;
    }
    return true;
  }

  private static boolean asciiLetterOrDigit(char character) {
    return (character >= 'a' && character <= 'z')
        || (character >= 'A' && character <= 'Z')
        || (character >= '0' && character <= '9');
  }

  private Context context() throws IOException, InterruptedException {
    if (!contextAttempted) {
      contextAttempted = true;
      if (runtime.getExecutableFile() == null) return null;
      List<TrackedFile> files = new ArrayList<>();
      files.add(new TrackedFile("installer", installer.toRealPath(), null, false));
      files.add(new TrackedFile("vanilla", vanillaJar.toRealPath(), null, false));
      files.add(
          new TrackedFile(
              "java", ProcessorProcessRunner.executablePath(runtime).toRealPath(), null, false));
      Fingerprint identity = new Fingerprint();
      identity.text(root.toRealPath().toString());
      byte[] runtimeIdentity = runtimeIdentity();
      identity.bytes(runtimeIdentity);
      for (TrackedFile file : files) {
        file.before = snapshot(file);
        if (file.before == null) return null;
        identity.text(file.label);
        identity.text(file.path.toString());
        identity.bytes(file.before.hash);
      }
      context = new Context(files, identity.finish(), runtimeIdentity);
    }
    return context;
  }

  private byte[] runtimeIdentity() throws IOException {
    Fingerprint identity = new Fingerprint();
    identity.text(ProcessorProcessRunner.executablePath(runtime).toRealPath().toString());
    identity.text(runtime.getVersion());
    identity.text(runtime.getVendor());
    identity.text(runtime.getOsArch());
    identity.text(runtime.getBitness());
    identity.number(runtime.is64Bit() ? 1 : 0);
    return identity.finish();
  }

  private final class Context {
    private final List<TrackedFile> files;
    private final byte[] identity;
    private final byte[] runtimeIdentity;
    private boolean reusable = true;

    private Context(List<TrackedFile> files, byte[] identity, byte[] runtimeIdentity) {
      this.files = files;
      this.identity = identity;
      this.runtimeIdentity = runtimeIdentity;
    }

    private boolean unchanged() throws IOException, InterruptedException {
      checkCancelled();
      if (!reusable) return false;
      if (!installer.toRealPath().equals(files.get(0).path)
          || !vanillaJar.toRealPath().equals(files.get(1).path)
          || runtime.getExecutableFile() == null
          || !Arrays.equals(runtimeIdentity, runtimeIdentity())) {
        reusable = false;
        return false;
      }
      // Warm checks share initial byte fingerprints within this locked invocation. Successful
      // executions additionally rehash these inputs before they can establish a new receipt.
      for (TrackedFile file : files) {
        checkCancelled();
        if (!file.before.sameAttributes(attributes(file))) {
          reusable = false;
          return false;
        }
      }
      return true;
    }

    private boolean unchangedBytes() throws IOException, InterruptedException {
      if (!unchanged()) return false;
      for (TrackedFile file : files) {
        FileState state = snapshot(file);
        if (state == null || !file.before.sameState(state)) {
          reusable = false;
          return false;
        }
      }
      return unchanged();
    }
  }

  final class Entry {
    private final Path receipt;
    private final byte[] identity;
    private final List<TrackedFile> tracked;
    private final List<TrackedFile> fixedFiles;
    private final Context fixed;

    private Entry(
        Path receipt,
        byte[] identity,
        List<TrackedFile> tracked,
        List<TrackedFile> fixedFiles,
        Context fixed) {
      this.receipt = receipt;
      this.identity = identity;
      this.tracked = tracked;
      this.fixedFiles = fixedFiles;
      this.fixed = fixed;
    }

    boolean isValid() throws IOException, InterruptedException {
      checkCancelled();
      try {
        Path path = checkedReceipt();
        BasicFileAttributes attributes =
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.size() > MAX_RECEIPT_BYTES) return false;
        try (DataInputStream input =
            new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)))) {
          if (input.readInt() != MAGIC || input.readInt() != VERSION) return false;
          byte[] hash = new byte[32];
          input.readFully(hash);
          if (!Arrays.equals(identity, hash) || input.readInt() != tracked.size()) return false;
          for (TrackedFile file : tracked) {
            checkCancelled();
            int length = input.readInt();
            if (length != file.labelBytes.length) return false;
            byte[] label = new byte[length];
            input.readFully(label);
            if (!Arrays.equals(label, file.labelBytes)) return false;
            input.readFully(hash);
            if (file.before == null || !Arrays.equals(file.before.hash, hash)) return false;
          }
          checkCancelled();
          return input.read() == -1;
        }
      } catch (IOException malformedOrMissing) {
        checkCancelled();
        return false;
      }
    }

    void invalidate() throws IOException, InterruptedException {
      checkCancelled();
      // This path comes only from the current recipe, never from receipt contents. Deleting a
      // final symlink deletes the metadata link itself, not whatever it might reference.
      Files.deleteIfExists(checkedReceipt());
    }

    void record() throws IOException, InterruptedException {
      checkCancelled();
      List<FileState> after = new ArrayList<>(tracked.size());
      boolean wroteArtifact = false;
      try {
        if (!fixed.unchanged()) return;
        for (TrackedFile file : fixedFiles) {
          FileState state = snapshot(file);
          if (state == null || !file.before.sameState(state)) return;
        }
        for (TrackedFile file : tracked) {
          FileState state = snapshot(file);
          if (state == null) return;
          if (file.maven) {
            wroteArtifact |= file.before == null || !file.before.sameState(state);
          } else if (!file.before.sameState(state)) {
            return;
          }
          after.add(state);
        }
        if (!wroteArtifact || !fixed.unchangedBytes()) return;
      } catch (IOException changedOrUnavailable) {
        checkCancelled();
        return;
      }
      checkCancelled();
      Path target = checkedReceipt();
      Path staged = InstallerArtifactStore.stage(target);
      try {
        InstallerArtifactStore.checkedPath(cache, staged);
        try (DataOutputStream output =
            new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(staged)))) {
          output.writeInt(MAGIC);
          output.writeInt(VERSION);
          output.write(identity);
          output.writeInt(tracked.size());
          for (int index = 0; index < tracked.size(); index++) {
            checkCancelled();
            byte[] label = tracked.get(index).labelBytes;
            output.writeInt(label.length);
            output.write(label);
            output.write(after.get(index).hash);
          }
        }
        checkCancelled();
        InstallerArtifactStore.publish(staged, checkedReceipt());
      } finally {
        Files.deleteIfExists(staged);
      }
    }

    private Path checkedReceipt() throws IOException {
      return ProcessorCache.this.checkedReceipt(receipt);
    }
  }

  private FileState snapshot(TrackedFile file) throws IOException, InterruptedException {
    checkCancelled();
    BasicFileAttributes before = attributes(file);
    if (before == null) return null;
    Path realPath = file.path.toRealPath();
    MessageDigest digest = sha256();
    if (hashBuffer == null) hashBuffer = new byte[64 * 1024];
    try (InputStream input = Files.newInputStream(file.path, LinkOption.NOFOLLOW_LINKS)) {
      int length;
      while ((length = input.read(hashBuffer)) != -1) {
        checkCancelled();
        digest.update(hashBuffer, 0, length);
      }
    }
    checkCancelled();
    FileState state = new FileState(before, realPath, digest.digest());
    if (!state.sameAttributes(attributes(file)) || !realPath.equals(file.path.toRealPath())) {
      throw new IOException("Processor cache input changed while hashing: " + file.path);
    }
    return state;
  }

  private BasicFileAttributes attributes(TrackedFile file) throws IOException {
    if (file.guard != null) {
      InstallerArtifactStore.checkedPath(root, file.guard);
      InstallerArtifactStore.checkedPath(file.guard, file.path);
    }
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(file.path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isRegularFile()) throw new IOException("Not a regular file: " + file.path);
      return attributes;
    } catch (NoSuchFileException missing) {
      return null;
    }
  }

  private static final class TrackedFile {
    private final String label;
    private final byte[] labelBytes;
    private final Path path;
    private final Path guard;
    private final boolean maven;
    private FileState before;

    private TrackedFile(String label, Path path, Path guard, boolean maven) {
      this.label = label;
      this.labelBytes = label.getBytes(StandardCharsets.UTF_8);
      this.path = path;
      this.guard = guard;
      this.maven = maven;
    }
  }

  private static final class FileState {
    private final BasicFileAttributes attributes;
    private final Path realPath;
    private final byte[] hash;

    private FileState(BasicFileAttributes attributes, Path realPath, byte[] hash) {
      this.attributes = attributes;
      this.realPath = realPath;
      this.hash = hash;
    }

    private boolean sameAttributes(BasicFileAttributes other) {
      return other != null
          && other.isRegularFile()
          && attributes.size() == other.size()
          && attributes.lastModifiedTime().equals(other.lastModifiedTime())
          && attributes.creationTime().equals(other.creationTime())
          && Objects.equals(attributes.fileKey(), other.fileKey());
    }

    private boolean sameState(FileState other) {
      return sameAttributes(other.attributes)
          && realPath.equals(other.realPath)
          && Arrays.equals(hash, other.hash);
    }
  }

  private static void profileIdentity(Fingerprint identity, ModernInstallerProfile profile) {
    identity.number(profile.getSpec());
    identity.text(profile.getProfile());
    identity.text(profile.getVersion());
    identity.text(profile.getMinecraft());
    identity.text(profile.getJson());
    identity.text(profile.getEmbeddedId());
    identity.text(profile.getEmbeddedParent());
    identity.bytes(profile.getVersionJsonBytes());
    identity.number(profile.getData().size());
    for (Map.Entry<String, ModernInstallerProfile.DataValue> data :
        new TreeMap<>(profile.getData()).entrySet()) {
      identity.text(data.getKey());
      identity.text(data.getValue().getClient());
      identity.text(data.getValue().getServer());
    }
    identity.number(profile.getProcessors().size());
    for (ModernInstallerProfile.Processor processor : profile.getProcessors()) {
      processorIdentity(identity, processor);
    }
    // Installer bytes bind the original library definitions; the verified classpath below binds
    // the actual selected library paths and contents, including architecture substitutions.
  }

  private static void processorIdentity(
      Fingerprint identity, ModernInstallerProfile.Processor processor) {
    identity.text(processor.getJar());
    identity.strings(processor.getClasspath());
    identity.strings(processor.getArgs());
    identity.strings(processor.getSides());
    identity.number(processor.getOutputs().size());
    for (Map.Entry<String, String> output : new TreeMap<>(processor.getOutputs()).entrySet()) {
      identity.text(output.getKey());
      identity.text(output.getValue());
    }
  }

  private static final class Fingerprint {
    private final MessageDigest digest = sha256();

    private void number(int value) {
      digest.update((byte) (value >>> 24));
      digest.update((byte) (value >>> 16));
      digest.update((byte) (value >>> 8));
      digest.update((byte) value);
    }

    private void text(String value) {
      if (value == null) number(-1);
      else bytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private void bytes(byte[] value) {
      number(value.length);
      digest.update(value);
    }

    private void strings(List<String> values) {
      number(values.size());
      for (String value : values) text(value);
    }

    private byte[] finish() {
      return digest.digest();
    }
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("SHA-256 is required by Java", unavailable);
    }
  }

  private void checkCancelled() throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new InterruptedException("Processor cache cancelled");
    }
  }
}
