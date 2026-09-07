package net.technicpack.minecraftcore.install.processor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.minecraftcore.install.processor.ModernInstallerArtifactResolver.ArtifactPlan;
import net.technicpack.minecraftcore.install.processor.ModernInstallerArtifactResolver.ArtifactRequest;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.utilslib.ZipUtils;

/** Executes the official client recipe while owning its repository lock and temporary resources. */
public final class ModernInstallerEngine {
  public void execute(Request request, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(reporter, "reporter");
    List<ModernInstallerProfile.Processor> processors = request.profile.getClientProcessors();
    if (processors.isEmpty()) return;

    checkCancelled(request);
    Files.createDirectories(request.root);
    Path cache = InstallerArtifactStore.checkedPath(request.root, request.root.resolve("cache"));
    Path libraries = request.root.resolve("libraries");
    try (ModernInstallerLock lock = ModernInstallerLock.acquire(cache, request.cancelled);
        WorkDirectory work = new WorkDirectory(cache)) {
      checkCancelled(request);
      ModernInstallerArtifactResolver resolver =
          new ModernInstallerArtifactResolver(request.root, request.installer, request.cancelled);
      // The earlier library node released its lock. Its result cannot authorize this run's inputs.
      List<ArtifactRequest> deferred =
          resolver.materialize(
              request.plan,
              new NodeProgressReporter() {
                @Override
                public void updateNodeProgress(float percent) {
                  reporter.updateNodeProgress(percent / 10);
                }

                @Override
                public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {
                  reporter.updateCurrentItem(label, mode, percent);
                }
              });
      checkCancelled(request);
      Map<String, String> data =
          InstallerTokenResolver.resolveData(
              request.profile, request.root, request.installer, request.vanillaJar, work.path);
      List<Invocation> invocations = new ArrayList<>(processors.size());
      Set<Path> declaredOutputs = new HashSet<>();
      for (ModernInstallerProfile.Processor processor : processors) {
        checkCancelled(request);
        Map<Path, String> outputs =
            InstallerTokenResolver.resolveOutputs(processor, data, request.root, libraries);
        List<String> arguments = new ArrayList<>(processor.getArgs().size());
        for (String argument : processor.getArgs()) {
          arguments.add(InstallerTokenResolver.resolveArgument(argument, data, libraries));
        }
        invocations.add(new Invocation(processor, arguments, outputs));
        declaredOutputs.addAll(outputs.keySet());
      }
      // Reject the entire recipe before any child starts, even when a later processor is at fault.
      for (ArtifactRequest artifact : deferred) {
        Path target = artifactPath(request, artifact);
        if (!declaredOutputs.contains(target)) {
          throw new IOException(
              "Unresolved artifact "
                  + artifact.getCoordinate()
                  + " is not an exact declared client output; attempted sources: "
                  + target
                  + ", "
                  + request.installer
                  + "!/maven/"
                  + artifact.getPath()
                  + ", "
                  + MavenCoordinate.resolve(cache, artifact.getPath())
                  + ", "
                  + MavenCoordinate.resolve(
                      Paths.get(System.getProperty("user.home"), ".m2", "repository"),
                      artifact.getPath())
                  + ", "
                  + artifact.getUrls());
        }
      }
      checkCancelled(request);
      Path bootstrap = ProcessorProcessRunner.createBootstrapJar(work.path);
      ProcessorProcessRunner runner = new ProcessorProcessRunner();
      for (int index = 0; index < invocations.size(); index++) {
        checkCancelled(request);
        Invocation invocation = invocations.get(index);
        reporter.updateCurrentItem(
            "(" + (index + 1) + "/" + invocations.size() + ") " + invocation.processor.getJar(),
            CurrentItemMode.INDETERMINATE,
            null);
        if (!prepareOutputs(request, invocation.outputs)) {
          List<Path> classpath = verifiedClasspath(request, resolver, invocation.processor);
          checkCancelled(request);
          runAndCheckOutputs(request, runner, work.path, bootstrap, invocation, classpath);
        }
        checkCancelled(request);
        reporter.updateNodeProgress(10 + (index + 1) * 90.0f / invocations.size());
      }
      for (ArtifactRequest artifact : request.plan.getRequests()) {
        checkCancelled(request);
        Path target = artifactPath(request, artifact);
        if (artifact.getSha1() == null || !declaredOutputs.contains(target)) continue;
        if (!isValid(request, target, artifact.getSha1())) {
          throw new IOException(
              "Generated artifact failed final SHA-1 verification: "
                  + artifact.getCoordinate()
                  + " at "
                  + target);
        }
      }
      checkCancelled(request);
    }
  }

  private static List<Path> verifiedClasspath(
      Request request,
      ModernInstallerArtifactResolver resolver,
      ModernInstallerProfile.Processor processor)
      throws IOException, InterruptedException {
    LinkedHashSet<Path> paths = new LinkedHashSet<>();
    verifyTool(request, resolver, processor.getJar(), paths);
    for (String dependency : processor.getClasspath()) {
      verifyTool(request, resolver, dependency, paths);
    }
    return new ArrayList<>(paths);
  }

  private static void verifyTool(
      Request request, ModernInstallerArtifactResolver resolver, String value, Set<Path> paths)
      throws IOException, InterruptedException {
    checkCancelled(request);
    MavenCoordinate coordinate;
    try {
      coordinate = MavenCoordinate.parse(value.replace("${arch}", request.runtime.getBitness()));
    } catch (IllegalArgumentException invalid) {
      throw new IOException("Invalid processor artifact: " + value, invalid);
    }
    ArtifactRequest artifact = request.plan.getRequest(coordinate.getPath());
    if (artifact == null || !artifact.getPath().equals(coordinate.getPath())) {
      throw new IOException(
          "Processor artifact is absent from the canonical artifact plan: " + value);
    }
    Path target = artifactPath(request, artifact);
    if (paths.contains(target)) return;
    String sha1 = resolver.verifiedSha1(artifact);
    if (sha1 == null || !sha1.matches("[0-9a-fA-F]{40}") || !isValid(request, target, sha1)) {
      throw new IOException(
          "Processor artifact failed SHA-1 verification: " + value + " at " + target);
    }
    checkCancelled(request);
    paths.add(target);
  }

  private static Path artifactPath(Request request, ArtifactRequest artifact) throws IOException {
    return InstallerArtifactStore.checkedPath(
        request.root,
        MavenCoordinate.resolve(request.root.resolve("libraries"), artifact.getPath()));
  }

  /** Empty output maps deliberately never authorize a skip. */
  private static boolean prepareOutputs(Request request, Map<Path, String> outputs)
      throws IOException, InterruptedException {
    boolean allValid = !outputs.isEmpty();
    for (Map.Entry<Path, String> output : outputs.entrySet()) {
      checkCancelled(request);
      Path target = InstallerArtifactStore.checkedPath(request.root, output.getKey());
      if (isValid(request, target, output.getValue())) continue;
      checkCancelled(request);
      Files.deleteIfExists(InstallerArtifactStore.checkedPath(request.root, target));
      if (restoreOutput(request, target, output.getValue())) continue;
      Files.createDirectories(InstallerArtifactStore.checkedPath(request.root, target).getParent());
      allValid = false;
    }
    return allValid;
  }

  private static boolean restoreOutput(Request request, Path target, String sha1)
      throws IOException, InterruptedException {
    Path libraries = request.root.resolve("libraries");
    if (target.equals(libraries) || !target.startsWith(libraries)) return false;
    String relative = libraries.relativize(target).toString().replace('\\', '/');
    SHA1FileVerifier verifier = new SHA1FileVerifier(sha1);
    checkCancelled(request);
    Path staged =
        InstallerArtifactStore.stage(InstallerArtifactStore.checkedPath(request.root, target));
    try {
      boolean extracted = false;
      try {
        ZipUtils.extractEntryTo(request.installer, "cache/" + relative, staged);
        extracted = true;
      } catch (IOException unavailable) {
        // Embedded caches are optional; only their exact, hash-valid entry is a usable candidate.
        checkCancelled(request);
      }
      checkCancelled(request);
      if (extracted && InstallerArtifactStore.isValid(staged, verifier)) {
        checkCancelled(request);
        InstallerArtifactStore.publish(
            staged, InstallerArtifactStore.checkedPath(request.root, target));
        return true;
      }
      checkCancelled(request);
    } catch (IOException | InterruptedException | RuntimeException | Error failure) {
      try {
        Files.deleteIfExists(staged);
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
    Files.deleteIfExists(staged);
    Path legacy =
        InstallerArtifactStore.checkedPath(
            request.root, MavenCoordinate.resolve(request.root.resolve("cache"), relative));
    boolean restored =
        InstallerArtifactStore.copyIfValid(
            legacy, InstallerArtifactStore.checkedPath(request.root, target), verifier);
    checkCancelled(request);
    return restored;
  }

  private static void runAndCheckOutputs(
      Request request,
      ProcessorProcessRunner runner,
      Path work,
      Path bootstrap,
      Invocation invocation,
      List<Path> classpath)
      throws IOException, InterruptedException {
    Throwable failure = null;
    try {
      runner.run(
          request.runtime,
          request.root,
          work,
          bootstrap,
          invocation.processor.getJar(),
          classpath,
          invocation.arguments,
          request.cancelled);
      checkCancelled(request);
    } catch (IOException | InterruptedException | RuntimeException | Error childFailure) {
      failure = childFailure;
      throw childFailure;
    } finally {
      // The runner restores interruption after child teardown. Hash valid files rather than
      // treating
      // an interrupted read as corruption, and never let cleanup replace the original failure.
      boolean interrupted = Thread.interrupted();
      try {
        IOException outputFailure = cleanAndCheckOutputs(request, invocation.outputs);
        if (outputFailure != null) {
          if (failure != null) failure.addSuppressed(outputFailure);
          else throw outputFailure;
        }
      } finally {
        if (interrupted) Thread.currentThread().interrupt();
      }
    }
  }

  private static IOException cleanAndCheckOutputs(Request request, Map<Path, String> outputs) {
    IOException failure = null;
    for (Map.Entry<Path, String> output : outputs.entrySet()) {
      try {
        Path target = InstallerArtifactStore.checkedPath(request.root, output.getKey());
        if (isValid(request, target, output.getValue())) continue;
        IOException invalid =
            new IOException(
                "Processor output is missing or has an invalid SHA-1: "
                    + target
                    + " (expected "
                    + output.getValue()
                    + ")");
        try {
          Files.deleteIfExists(InstallerArtifactStore.checkedPath(request.root, target));
        } catch (IOException cleanupFailure) {
          invalid.addSuppressed(cleanupFailure);
        }
        if (failure == null) failure = invalid;
        else failure.addSuppressed(invalid);
      } catch (IOException unsafe) {
        if (failure == null) failure = unsafe;
        else failure.addSuppressed(unsafe);
      }
    }
    return failure;
  }

  private static boolean isValid(Request request, Path target, String sha1) throws IOException {
    return InstallerArtifactStore.isValid(
        InstallerArtifactStore.checkedPath(request.root, target), new SHA1FileVerifier(sha1));
  }

  private static void checkCancelled(Request request) throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || request.cancelled.getAsBoolean()) {
      throw new InterruptedException("Modern installer processor execution cancelled");
    }
  }

  private static final class Invocation {
    private final ModernInstallerProfile.Processor processor;
    private final List<String> arguments;
    private final Map<Path, String> outputs;

    private Invocation(
        ModernInstallerProfile.Processor processor,
        List<String> arguments,
        Map<Path, String> outputs) {
      this.processor = processor;
      this.arguments = arguments;
      this.outputs = outputs;
    }
  }

  private static final class WorkDirectory implements AutoCloseable {
    private final Path path;

    private WorkDirectory(Path cache) throws IOException {
      path = Files.createTempDirectory(cache, "modern-installer-");
    }

    @Override
    public void close() throws IOException {
      // Default walkFileTree does not follow child-created symlinks outside this owned directory.
      Files.walkFileTree(
          path,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
              Files.deleteIfExists(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException failure)
                throws IOException {
              if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return FileVisitResult.CONTINUE;
              throw failure;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                throws IOException {
              if (failure != null) throw failure;
              Files.deleteIfExists(directory);
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  /** All mutable execution state is owned by execute, never retained between planner nodes. */
  public static final class Request {
    private final Path root;
    private final Path installer;
    private final Path vanillaJar;
    private final ModernInstallerProfile profile;
    private final ArtifactPlan plan;
    private final IJavaRuntime runtime;
    private final BooleanSupplier cancelled;

    public Request(
        Path root,
        Path installer,
        Path vanillaJar,
        ModernInstallerProfile profile,
        ArtifactPlan plan,
        IJavaRuntime runtime,
        BooleanSupplier cancelled) {
      this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
      this.installer = Objects.requireNonNull(installer, "installer").toAbsolutePath().normalize();
      this.vanillaJar =
          Objects.requireNonNull(vanillaJar, "vanillaJar").toAbsolutePath().normalize();
      this.profile = Objects.requireNonNull(profile, "profile");
      this.plan = Objects.requireNonNull(plan, "plan");
      this.runtime = Objects.requireNonNull(runtime, "runtime");
      this.cancelled = Objects.requireNonNull(cancelled, "cancelled");
    }
  }
}
