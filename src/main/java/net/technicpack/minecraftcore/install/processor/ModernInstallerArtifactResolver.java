package net.technicpack.minecraftcore.install.processor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.install.plan.actions.DownloadFilePlanAction;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.minecraftcore.mojang.version.io.Artifact;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import net.technicpack.utilslib.ZipUtils;

/** Plans and acquires exact artifacts without treating generated paths as downloadable inputs. */
public final class ModernInstallerArtifactResolver {
  private static final Pattern SIDECAR =
      Pattern.compile("([0-9a-fA-F]{40})(?:[ \\t]+\\*?([^\\r\\n]+))?");
  private static final int MAX_SIDECAR_BYTES = 4096;
  private final Path root;
  private final Path installer;
  private final BooleanSupplier cancelled;

  public ModernInstallerArtifactResolver(
      Path launcherRoot, Path installer, BooleanSupplier cancelled) {
    this.root = Objects.requireNonNull(launcherRoot, "launcherRoot").toAbsolutePath().normalize();
    this.installer = Objects.requireNonNull(installer, "installer").toAbsolutePath().normalize();
    this.cancelled = Objects.requireNonNull(cancelled, "cancelled");
  }

  /**
   * The caller holds {@link ModernInstallerLock} throughout this method, including all staging.
   * Only declared empty-URL artifacts may be deferred, and only for a client processor sequence.
   */
  public List<ArtifactRequest> materialize(ArtifactPlan plan, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(reporter, "reporter");
    List<ArtifactRequest> deferred = new ArrayList<>();
    int index = 0;
    for (ArtifactRequest request : plan.requests) {
      checkCancelled();
      final int item = index;
      NodeProgressReporter itemReporter =
          new NodeProgressReporter() {
            @Override
            public void updateNodeProgress(float percent) {
              reporter.updateNodeProgress((item * 100.0f + percent) / plan.requests.size());
            }

            @Override
            public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {
              reporter.updateCurrentItem(label, mode, percent);
            }
          };
      itemReporter.updateCurrentItem(
          request.coordinate.toString(), CurrentItemMode.INDETERMINATE, null);
      if (!materialize(request, plan.clientProcessors, itemReporter)) deferred.add(request);
      checkCancelled();
      reporter.updateNodeProgress(++index * 100.0f / plan.requests.size());
    }
    checkCancelled();
    return Collections.unmodifiableList(deferred);
  }

  /**
   * Returns declared integrity metadata, or the validated adjacent inferred digest. The engine must
   * still verify the artifact's content against this digest before starting a processor.
   */
  public String verifiedSha1(ArtifactRequest request) throws IOException {
    if (!request.inferred) return request.sha1;
    Path target = target(request);
    Path sidecar = checked(target.resolveSibling(target.getFileName() + ".sha1"));
    String sha1 = readSidecar(sidecar, target);
    if (sha1 == null) {
      throw new IOException("Missing or malformed local SHA-1 sidecar for " + request.coordinate);
    }
    return sha1;
  }

  private boolean materialize(
      ArtifactRequest request, boolean clientProcessors, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    Path target = target(request);
    Path sidecar =
        request.inferred ? checked(target.resolveSibling(target.getFileName() + ".sha1")) : null;
    String sha1 = request.inferred ? readSidecar(sidecar, target) : request.sha1;
    if (request.emptyUrl && sha1 == null && !request.inferred) {
      throw new IOException("Missing declared SHA-1 for " + request.coordinate);
    }
    IFileVerifier verifier = null;
    if (!request.inferred || sha1 != null) {
      verifier =
          cancellableVerifier(
              sha1 == null ? new ValidZipFileVerifier() : new SHA1FileVerifier(sha1));
      boolean valid = InstallerArtifactStore.isValid(target, verifier);
      checkCancelled();
      if (valid) return true;
    }
    Path legacy = MavenCoordinate.resolve(root.resolve("cache"), request.path);
    Path mavenLocal =
        MavenCoordinate.resolve(
            Paths.get(System.getProperty("user.home"), ".m2", "repository"), request.path);
    IOException failure =
        new IOException(
            "Cannot acquire artifact "
                + request.coordinate
                + "; attempted sources: "
                + target
                + ", "
                + installer
                + "!/maven/"
                + request.path
                + ", "
                + legacy
                + ", "
                + mavenLocal
                + ", "
                + request.urls);
    Path stagedSidecar = null;
    try {
      boolean acquired = false;
      if (request.inferred && sha1 == null) {
        stagedSidecar = InstallerArtifactStore.stage(sidecar);
        IFileVerifier sidecarVerifier =
            new IFileVerifier() {
              @Override
              public boolean isFileValid(File file) {
                return isFileValid(file.toPath());
              }

              @Override
              public boolean isFileValid(Path path) {
                return readSidecar(path, target) != null;
              }
            };
        for (String url : request.urls) {
          checkCancelled();
          try {
            download(url + ".sha1", stagedSidecar, sidecarVerifier, request, reporter);
            sha1 = readSidecar(stagedSidecar, target);
            if (sha1 != null) break;
          } catch (IOException invalid) {
            checkCancelled();
            failure.addSuppressed(invalid);
          }
        }
        if (sha1 == null) {
          throw new IOException(
              "No valid official SHA-1 sidecar for " + request.coordinate, failure);
        }
        verifier = cancellableVerifier(new SHA1FileVerifier(sha1));
        acquired = InstallerArtifactStore.isValid(target, verifier);
        checkCancelled();
      }
      if (!acquired) acquired = extractEmbedded(request, target, verifier, failure);
      if (!acquired) acquired = copyCandidate(checked(legacy), target, verifier, failure);
      if (!acquired) acquired = copyCandidate(mavenLocal, target, verifier, failure);
      if (!acquired) {
        for (String url : request.urls) {
          checkCancelled();
          IFileVerifier downloadVerifier = verifier;
          if (sha1 == null) {
            String md5 = Utils.getETag(url);
            checkCancelled();
            if (md5 != null && !md5.isEmpty()) {
              downloadVerifier = cancellableVerifier(new MD5FileVerifier(md5));
            }
          }
          Path staged = InstallerArtifactStore.stage(checked(target));
          try {
            download(url, staged, downloadVerifier, request, reporter);
            checkCancelled();
            InstallerArtifactStore.publish(staged, checked(target));
            acquired = true;
            break;
          } catch (IOException invalid) {
            checkCancelled();
            failure.addSuppressed(invalid);
          } finally {
            Files.deleteIfExists(staged);
          }
        }
      }
      checkCancelled();
      if (!acquired) {
        if (clientProcessors && request.emptyUrl && !request.inferred) return false;
        throw failure;
      }
      if (stagedSidecar != null) {
        // Do not replace cached metadata when the artifact itself could not be acquired.
        checkCancelled();
        InstallerArtifactStore.publish(stagedSidecar, checked(sidecar));
      }
      return true;
    } finally {
      if (stagedSidecar != null) Files.deleteIfExists(stagedSidecar);
    }
  }

  private boolean extractEmbedded(
      ArtifactRequest request, Path target, IFileVerifier verifier, IOException failure)
      throws IOException, InterruptedException {
    checkCancelled();
    Path staged = InstallerArtifactStore.stage(checked(target));
    try {
      ZipUtils.extractEntryTo(installer, "maven/" + request.path, staged);
      checkCancelled();
      if (!InstallerArtifactStore.isValid(staged, verifier)) {
        checkCancelled();
        failure.addSuppressed(new IOException("Invalid installer artifact maven/" + request.path));
        return false;
      }
      checkCancelled();
      InstallerArtifactStore.publish(staged, checked(target));
      return true;
    } catch (IOException invalid) {
      checkCancelled();
      failure.addSuppressed(invalid);
      return false;
    } finally {
      Files.deleteIfExists(staged);
    }
  }

  private boolean copyCandidate(
      Path source, Path target, IFileVerifier verifier, IOException failure)
      throws IOException, InterruptedException {
    checkCancelled();
    try {
      return InstallerArtifactStore.copyIfValid(source, checked(target), verifier);
    } catch (IOException invalid) {
      checkCancelled();
      failure.addSuppressed(invalid);
      return false;
    }
  }

  private void download(
      String url,
      Path staged,
      IFileVerifier verifier,
      ArtifactRequest request,
      NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    checkCancelled();
    Thread owner = Thread.currentThread();
    class DownloadReporter implements NodeProgressReporter {
      private boolean active = true;

      private synchronized void checkCancellation() {
        // DownloadFilePlanAction also reports from its transfer monitor thread.
        if (active && cancelled.getAsBoolean()) owner.interrupt();
      }

      private synchronized void close() {
        active = false;
      }

      @Override
      public void updateNodeProgress(float percent) {
        checkCancellation();
        reporter.updateNodeProgress(percent);
      }

      @Override
      public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {
        checkCancellation();
        reporter.updateCurrentItem(label, mode, percent);
      }
    }
    DownloadReporter interruptible = new DownloadReporter();
    try {
      new DownloadFilePlanAction<Void>(
              url, staged.toFile(), verifier, request.coordinate.toString())
          .withTaskDescriptionAsProgressLabel()
          .execute(null, interruptible);
      checkCancelled();
      if (!InstallerArtifactStore.isValid(staged, verifier)) {
        checkCancelled();
        throw new IOException("Downloaded artifact failed verification: " + url);
      }
      checkCancelled();
    } finally {
      interruptible.close();
    }
  }

  private IFileVerifier cancellableVerifier(IFileVerifier verifier) {
    return new IFileVerifier() {
      @Override
      public boolean isFileValid(File file) {
        return isFileValid(file.toPath());
      }

      @Override
      public boolean isFileValid(Path path) {
        if (cancelled.getAsBoolean()) Thread.currentThread().interrupt();
        if (Thread.currentThread().isInterrupted()) return false;
        boolean valid = verifier.isFileValid(path);
        if (cancelled.getAsBoolean()) Thread.currentThread().interrupt();
        return valid && !Thread.currentThread().isInterrupted();
      }
    };
  }

  public static String readSidecar(Path sidecar, Path target) {
    try {
      if (!Files.isRegularFile(sidecar) || Files.size(sidecar) > MAX_SIDECAR_BYTES) return null;
      String text = new String(Files.readAllBytes(sidecar), StandardCharsets.US_ASCII).trim();
      Matcher match = SIDECAR.matcher(text);
      if (!match.matches()
          || match.group(2) != null && !match.group(2).equals(target.getFileName().toString()))
        return null;
      return match.group(1).toLowerCase(Locale.ROOT);
    } catch (IOException unreadable) {
      return null;
    }
  }

  private Path target(ArtifactRequest request) throws IOException {
    return checked(MavenCoordinate.resolve(root.resolve("libraries"), request.path));
  }

  private Path checked(Path path) throws IOException {
    return InstallerArtifactStore.checkedPath(root, path);
  }

  private void checkCancelled() throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
      throw new InterruptedException("Modern installer artifact acquisition cancelled");
    }
  }

  /** Effective libraries have already been selected using the game's launch options and runtime. */
  public static ArtifactPlan plan(
      ModernInstallerProfile profile, List<Library> effectiveLibraries, IJavaRuntime runtime)
      throws IOException {
    Map<String, ArtifactRequest> requests = new LinkedHashMap<>();
    boolean clientProcessors = !profile.getClientProcessors().isEmpty();
    try {
      Set<MavenCoordinate> tools = new LinkedHashSet<>();
      if (clientProcessors) {
        for (ModernInstallerProfile.Processor processor : profile.getClientProcessors()) {
          tools.add(MavenCoordinate.parse(substitute(processor.getJar(), runtime)));
          for (String dependency : processor.getClasspath()) {
            tools.add(MavenCoordinate.parse(substitute(dependency, runtime)));
          }
        }
      }
      for (Library library : effectiveLibraries) {
        if (!library.isLocal()) addLibrary(requests, library, runtime, true, tools);
      }
      if (clientProcessors) {
        for (Library library : profile.getLibraries()) {
          if (library.isLocal())
            throw new IOException("Installer tool cannot be pack-local: " + library.getName());
          addLibrary(requests, library, runtime, false, tools);
        }
        for (MavenCoordinate tool : tools) addTool(requests, tool);
      }
    } catch (IllegalArgumentException malformed) {
      throw new IOException("Invalid modern installer artifact", malformed);
    }
    return new ArtifactPlan(requests, clientProcessors);
  }

  private static void addLibrary(
      Map<String, ArtifactRequest> requests,
      Library library,
      IJavaRuntime runtime,
      boolean game,
      Set<MavenCoordinate> tools)
      throws IOException {
    String classifier =
        library.resolveNativeClassifier(
            OperatingSystem.getOperatingSystem().getName(), runtime.getOsArch());
    if (classifier != null) classifier = substitute(classifier, runtime);
    MavenCoordinate coordinate = MavenCoordinate.parse(substitute(library.getName(), runtime));
    if (classifier != null) coordinate = coordinate.withClassifier(classifier);
    String path = substitute(library.getArtifactPath(classifier), runtime);
    MavenCoordinate.resolve(Paths.get(""), path);
    if (tools.contains(coordinate) && !path.equals(coordinate.getPath())) {
      throw new IOException(
          "Processor artifact path is not canonical: " + coordinate + " -> " + path);
    }
    Artifact metadata = library.getArtifact(classifier);
    String sha1 = metadata == null ? null : metadata.getSha1();
    long size = metadata == null ? 0 : metadata.getSize();
    if (metadata != null && (sha1 == null || !sha1.matches("[0-9a-fA-F]{40}"))) {
      throw new IOException(
          "Missing or malformed SHA-1 for declared artifact " + library.getName());
    }
    if (size < 0) throw new IOException("Negative artifact size: " + library.getName());
    boolean emptyUrl =
        metadata != null && (metadata.getUrl() == null || metadata.getUrl().isEmpty());
    List<String> urls = new ArrayList<>();
    if (!emptyUrl) {
      for (String url : library.getDownloadCandidates(path, classifier)) {
        urls.add(substitute(url, runtime));
      }
    }
    add(requests, new ArtifactRequest(coordinate, path, sha1, size, urls, game, false, emptyUrl));
  }

  private static void addTool(Map<String, ArtifactRequest> requests, MavenCoordinate coordinate)
      throws IOException {
    ArtifactRequest existing = requests.get(coordinate.getPath());
    if (existing != null) {
      if (existing.sha1 == null && !existing.inferred) {
        throw new IOException("Processor dependency has no declared SHA-1: " + coordinate);
      }
      return;
    }
    add(
        requests,
        new ArtifactRequest(
            coordinate,
            coordinate.getPath(),
            null,
            0,
            Collections.singletonList(repositoryFor(coordinate) + coordinate.getPath()),
            false,
            true,
            false));
  }

  private static void add(Map<String, ArtifactRequest> requests, ArtifactRequest next)
      throws IOException {
    ArtifactRequest previous = requests.get(next.path);
    if (previous == null) {
      requests.put(next.path, next);
      return;
    }
    if (previous.sha1 != null && next.sha1 != null && !previous.sha1.equalsIgnoreCase(next.sha1)
        || previous.size > 0 && next.size > 0 && previous.size != next.size) {
      throw new IOException("Conflicting artifact metadata at " + next.path);
    }
    LinkedHashSet<String> urls = new LinkedHashSet<>(previous.urls);
    urls.addAll(next.urls);
    requests.put(
        next.path,
        new ArtifactRequest(
            previous.coordinate,
            next.path,
            previous.sha1 != null ? previous.sha1 : next.sha1,
            previous.size > 0 ? previous.size : next.size,
            new ArrayList<>(urls),
            previous.gameLibrary || next.gameLibrary,
            previous.inferred && next.inferred,
            previous.emptyUrl && next.emptyUrl));
  }

  static String repositoryFor(MavenCoordinate coordinate) {
    String group = coordinate.getGroup();
    if (group.equals("net.minecraftforge") || group.startsWith("net.minecraftforge.")) {
      return "https://maven.minecraftforge.net/";
    }
    if (group.equals("net.neoforged") || group.startsWith("net.neoforged.")) {
      return "https://maven.neoforged.net/releases/";
    }
    if (group.equals("com.mojang")
        || group.startsWith("com.mojang.")
        || group.equals("net.minecraft")
        || group.startsWith("net.minecraft.")) {
      return "https://libraries.minecraft.net/";
    }
    return "https://repo.maven.apache.org/maven2/";
  }

  private static String substitute(String value, IJavaRuntime runtime) {
    return value.replace("${arch}", runtime.getBitness());
  }

  public static final class ArtifactPlan {
    private final Map<String, ArtifactRequest> byPath;
    private final List<ArtifactRequest> requests;
    private final List<ArtifactRequest> gameArtifacts;
    private final List<ArtifactRequest> installOnlyArtifacts;
    private final boolean clientProcessors;

    ArtifactPlan(Map<String, ArtifactRequest> byPath, boolean clientProcessors) {
      this.byPath = Collections.unmodifiableMap(new LinkedHashMap<>(byPath));
      this.requests = Collections.unmodifiableList(new ArrayList<>(byPath.values()));
      List<ArtifactRequest> game = new ArrayList<>();
      List<ArtifactRequest> tools = new ArrayList<>();
      for (ArtifactRequest request : requests) {
        (request.gameLibrary ? game : tools).add(request);
      }
      gameArtifacts = Collections.unmodifiableList(game);
      installOnlyArtifacts = Collections.unmodifiableList(tools);
      this.clientProcessors = clientProcessors;
    }

    public List<ArtifactRequest> getRequests() {
      return requests;
    }

    public List<ArtifactRequest> getGameArtifacts() {
      return gameArtifacts;
    }

    public List<ArtifactRequest> getInstallOnlyArtifacts() {
      return installOnlyArtifacts;
    }

    public ArtifactRequest getRequest(String path) {
      return byPath.get(path);
    }

    public boolean hasClientProcessors() {
      return clientProcessors;
    }
  }

  public static final class ArtifactRequest {
    private final MavenCoordinate coordinate;
    private final String path;
    private final String sha1;
    private final long size;
    private final List<String> urls;
    private final boolean gameLibrary;
    private final boolean inferred;
    private final boolean emptyUrl;

    ArtifactRequest(
        MavenCoordinate coordinate,
        String path,
        String sha1,
        long size,
        List<String> urls,
        boolean gameLibrary,
        boolean inferred,
        boolean emptyUrl) {
      this.coordinate = coordinate;
      this.path = path;
      this.sha1 = sha1 == null ? null : sha1.toLowerCase(Locale.ROOT);
      this.size = size;
      this.urls = Collections.unmodifiableList(new ArrayList<>(urls));
      this.gameLibrary = gameLibrary;
      this.inferred = inferred;
      this.emptyUrl = emptyUrl;
    }

    public MavenCoordinate getCoordinate() {
      return coordinate;
    }

    public String getPath() {
      return path;
    }

    public String getSha1() {
      return sha1;
    }

    public long getSize() {
      return size;
    }

    public List<String> getUrls() {
      return urls;
    }

    public boolean isGameLibrary() {
      return gameLibrary;
    }

    public boolean isInferred() {
      return inferred;
    }

    public boolean isEmptyUrl() {
      return emptyUrl;
    }
  }
}
