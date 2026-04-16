package net.technicpack.launcher.launch;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.JavaVersionComparator;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.LegacyTaskPlanAction;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.install.plan.PlanBuilder;
import net.technicpack.launchercore.install.plan.actions.DownloadFilePlanAction;
import net.technicpack.launchercore.install.tasks.CopyFileTask;
import net.technicpack.launchercore.install.tasks.EnsureLinkedFileTask;
import net.technicpack.launchercore.install.tasks.UnzipFileTask;
import net.technicpack.launchercore.install.tasks.WriteRundataFile;
import net.technicpack.launchercore.install.verifiers.FileSizeVerifier;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidJsonFileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.FmlLibsManager;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.install.ExtractedFilesManifest;
import net.technicpack.minecraftcore.install.ModpackZipFilter;
import net.technicpack.minecraftcore.install.PrismInstanceRemapper;
import net.technicpack.minecraftcore.install.tasks.CleanupModpackCacheTask;
import net.technicpack.minecraftcore.install.tasks.InstallMinecraftIfNecessaryTask;
import net.technicpack.minecraftcore.install.tasks.RenameJnilibToDylibTask;
import net.technicpack.minecraftcore.mojang.java.JavaRuntime;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeFile;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeFileType;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimeManifest;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimesIndex;
import net.technicpack.minecraftcore.mojang.version.ExtractRulesFileFilter;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.MinecraftVersionInfoBuilder;
import net.technicpack.minecraftcore.mojang.version.io.Artifact;
import net.technicpack.minecraftcore.mojang.version.io.AssetIndex;
import net.technicpack.minecraftcore.mojang.version.io.Download;
import net.technicpack.minecraftcore.mojang.version.io.Downloads;
import net.technicpack.minecraftcore.mojang.version.io.ExtractRules;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import net.technicpack.minecraftcore.mojang.version.io.VersionJavaInfo;
import net.technicpack.minecraftcore.mojang.version.io.VersionPatch;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.rest.io.Mod;
import net.technicpack.rest.io.Modpack;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.IZipFileFilter;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

class ImmutableInstallerPlanner {
  private static final String PREPARE_PHASE = "prepare-modpack";
  private static final String INSTALL_MODS_PHASE = "install-mods";
  private static final String RUN_DATA_PHASE = "run-data";
  private static final String CHECK_VERSION_PHASE = "check-version";
  private static final String EXAMINE_VERSION_PHASE = "examine-version";
  private static final String INSTALL_LIBS_PHASE = "install-libs";
  private static final String INSTALL_MINECRAFT_PHASE = "install-minecraft";
  private static final String INSTALL_ASSETS_PHASE = "install-assets";
  private static final String INSTALL_JAVA_PHASE = "install-java";
  private static final String FIX_NATIVE_PHASE = "fix-natives";

  private static final String VIRTUAL_FIELD = "virtual";
  private static final String MAP_TO_RESOURCES_FIELD = "map_to_resources";
  private static final String OBJECTS_FIELD = "objects";
  private static final String SIZE_FIELD = "size";
  private static final String HASH_FIELD = "hash";

  private final ResourceLoader resources;
  private final ModpackModel pack;
  private final Modpack modpackData;
  private final LauncherFileSystem fileSystem;
  private final MinecraftVersionInfoBuilder versionBuilder;
  private final TechnicSettings settings;
  private final IJavaRuntime selectedJavaRuntime;
  private final boolean doFullInstall;
  private final boolean mojangJavaWanted;
  private final boolean jarRegenerationRequired;
  private final String minecraftVersion;
  private final BooleanSupplier cancellationCheck;

  // Set during mod extraction if a Prism instance zip is detected
  private PrismInstanceRemapper detectedPrismRemapper;
  // Effective minimum Java major across all version patches: the maximum of every patch's
  // minimum compatibleJavaMajors value, so the most-restrictive minimum wins. Written to
  // bin/runData (Technic's runtime-constraints file) when values were derived from a Prism
  // instance zip rather than the Solder API; 0 means "unset" (no patches contributed).
  private int patchEffectiveMinJavaMajor;
  // Collects all file paths extracted from mod zips (for orphan cleanup)
  private final Set<String> allExtractedPaths = new LinkedHashSet<>();

  ImmutableInstallerPlanner(
      ResourceLoader resources,
      ModpackModel pack,
      Modpack modpackData,
      LauncherFileSystem fileSystem,
      MinecraftVersionInfoBuilder versionBuilder,
      TechnicSettings settings,
      IJavaRuntime selectedJavaRuntime,
      boolean doFullInstall,
      boolean mojangJavaWanted,
      boolean jarRegenerationRequired,
      BooleanSupplier cancellationCheck) {
    this.resources = resources;
    this.pack = pack;
    this.modpackData = modpackData;
    this.fileSystem = fileSystem;
    this.versionBuilder = versionBuilder;
    this.settings = settings;
    this.selectedJavaRuntime = selectedJavaRuntime;
    this.doFullInstall = doFullInstall;
    this.mojangJavaWanted = mojangJavaWanted;
    this.jarRegenerationRequired = jarRegenerationRequired;
    this.minecraftVersion = modpackData.getGameVersion();
    this.cancellationCheck = cancellationCheck;
  }

  ExecutionPlan<InstallExecutionContext> buildPreparationPlan() {
    PlanBuilder<InstallExecutionContext> builder = new PlanBuilder<>();
    builder.addPhase(PREPARE_PHASE, resources.getString("install.message.examiningmodpack"));
    builder.addPhase(INSTALL_MODS_PHASE, resources.getString("install.message.installmods"));
    builder.addPhase(RUN_DATA_PHASE, resources.getString("install.message.runData"));

    if (doFullInstall) {
      builder.addNode(
          "cleanup-modpack",
          PREPARE_PHASE,
          "Wiping Folders",
          1.0f,
          (context, reporter) -> cleanupModpackDirectories());
      builder.addNode(
          "install-modpack-contents",
          INSTALL_MODS_PHASE,
          "Installing Modpack Contents",
          Math.max(1.0f, modpackData.getMods().size()),
          Collections.singletonList("cleanup-modpack"),
          (context, reporter) -> installModpackContents(reporter));
      builder.addNode(
          "cleanup-orphaned-files",
          INSTALL_MODS_PHASE,
          "Cleaning orphaned files",
          1.0f,
          Collections.singletonList("install-modpack-contents"),
          (context, reporter) -> cleanupOrphanedFiles());
      builder.addNode(
          "cleanup-modpack-cache",
          INSTALL_MODS_PHASE,
          "Cleaning Modpack Cache",
          1.0f,
          Collections.singletonList("cleanup-orphaned-files"),
          new LegacyTaskPlanAction<InstallExecutionContext, IMinecraftVersionInfo>(
              new CleanupModpackCacheTask(pack, modpackData),
              InstallExecutionContext::getResolvedVersion));
    }

    if (shouldWriteRunData()) {
      Collection<String> dependencies =
          doFullInstall
              ? Collections.singletonList("cleanup-modpack-cache")
              : Collections.<String>emptyList();
      builder.addNode(
          "write-run-data",
          RUN_DATA_PHASE,
          "Writing Runtime Data",
          1.0f,
          dependencies,
          new LegacyTaskPlanAction<InstallExecutionContext, IMinecraftVersionInfo>(
              new WriteRundataFile(pack, modpackData),
              InstallExecutionContext::getResolvedVersion));
    }

    return builder.build();
  }

  ExecutionPlan<InstallExecutionContext> buildVersionDiscoveryPlan() {
    PlanBuilder<InstallExecutionContext> builder = new PlanBuilder<>();
    builder.addPhase(CHECK_VERSION_PHASE, resources.getString("install.message.checkversionfile"));
    builder.addPhase(
        EXAMINE_VERSION_PHASE, resources.getString("install.message.examiningversionfile"));

    builder.addNode(
        "resolve-version",
        CHECK_VERSION_PHASE,
        "Retrieving Modpack Version",
        1.0f,
        (context, reporter) -> context.setResolvedVersion(resolveVersion()));
    builder.addNode(
        "apply-patches",
        EXAMINE_VERSION_PHASE,
        "Applying version patches",
        1.0f,
        Collections.singletonList("resolve-version"),
        (context, reporter) -> applyVersionPatches(context));
    builder.addNode(
        "write-prism-rundata",
        EXAMINE_VERSION_PHASE,
        "Writing Prism runtime data",
        1.0f,
        Collections.singletonList("apply-patches"),
        (context, reporter) -> writePrismRunData());
    builder.addNode(
        "prepare-version",
        EXAMINE_VERSION_PHASE,
        "Processing version.",
        1.0f,
        Collections.singletonList("write-prism-rundata"),
        (context, reporter) -> prepareResolvedVersion(context));
    return builder.build();
  }

  ExecutionPlan<InstallExecutionContext> buildInstallPlan(InstallExecutionContext context)
      throws IOException {
    Objects.requireNonNull(
        context.getResolvedVersion(), "Resolved version must exist before building install plan");

    PlanBuilder<InstallExecutionContext> builder = new PlanBuilder<>();
    builder.addPhase(INSTALL_LIBS_PHASE, resources.getString("install.message.installlibs"));
    builder.addPhase(
        INSTALL_MINECRAFT_PHASE, resources.getString("install.message.installminecraft"));
    builder.addPhase(INSTALL_ASSETS_PHASE, resources.getString("install.message.installassets"));
    if (mojangJavaWanted && context.getResolvedVersion().getMojangRuntimeInformation() != null) {
      builder.addPhase(INSTALL_JAVA_PHASE, "Downloading Java runtime...");
    }
    if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX) {
      builder.addPhase(FIX_NATIVE_PHASE, "Fixing OSX natives");
    }

    Map<String, String> fmlLibs = FmlLibsManager.getLibsForVersion(minecraftVersion);
    if (!fmlLibs.isEmpty()) {
      builder.addNode(
          "install-fml-libraries",
          INSTALL_LIBS_PHASE,
          "Installing FML libraries",
          Math.max(1.0f, fmlLibs.size()),
          (installContext, reporter) -> installFmlLibraries(fmlLibs, reporter));
    }

    if (!context.getLibrariesToInstall().isEmpty()) {
      builder.addNode(
          "install-version-libraries",
          INSTALL_LIBS_PHASE,
          "Installing Version Libraries",
          Math.max(1.0f, context.getLibrariesToInstall().size()),
          (installContext, reporter) -> installVersionLibraries(installContext, reporter));
    }

    builder.addNode(
        "install-minecraft",
        INSTALL_MINECRAFT_PHASE,
        "Installing Minecraft",
        1.0f,
        new LegacyTaskPlanAction<InstallExecutionContext, IMinecraftVersionInfo>(
            new InstallMinecraftIfNecessaryTask(
                pack, minecraftVersion, fileSystem.getCacheDirectory(), jarRegenerationRequired),
            InstallExecutionContext::getResolvedVersion));

    builder.addNode(
        "install-assets",
        INSTALL_ASSETS_PHASE,
        "Checking Minecraft Assets",
        1.0f,
        (installContext, reporter) -> installAssets(installContext, reporter));

    if (mojangJavaWanted && context.getResolvedVersion().getMojangRuntimeInformation() != null) {
      builder.addNode(
          "install-java-runtime",
          INSTALL_JAVA_PHASE,
          "Installing Java runtime",
          1.0f,
          (installContext, reporter) -> installJavaRuntime(installContext, reporter));
    }

    if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX) {
      Collection<String> renameDependencies =
          context.getLibrariesToInstall().isEmpty()
              ? Collections.<String>emptyList()
              : Collections.singletonList("install-version-libraries");
      builder.addNode(
          "rename-jnilib",
          FIX_NATIVE_PHASE,
          "Fixing OSX natives",
          1.0f,
          renameDependencies,
          new LegacyTaskPlanAction<InstallExecutionContext, IMinecraftVersionInfo>(
              new RenameJnilibToDylibTask(pack), InstallExecutionContext::getResolvedVersion));
    }

    return builder.build();
  }

  private boolean shouldWriteRunData() {
    if (doFullInstall) {
      return true;
    }

    if (pack.isLocalOnly()) {
      return false;
    }

    return !new File(pack.getBinDir(), "runData").exists();
  }

  private void cleanupModpackDirectories() throws IOException {
    final File binDir = pack.getBinDir();

    removeFile(new File(binDir, "version.json"));
    removeFile(new File(binDir, "install_profile.json"));

    File[] binFiles = binDir.listFiles();
    if (binFiles != null) {
      final Pattern minecraftVersionPattern = Pattern.compile("^\\d++(\\.\\d++)++\\.json$");
      for (File binFile : binFiles) {
        if (minecraftVersionPattern.matcher(binFile.getName()).matches()) {
          removeFile(binFile);
        }
      }
    }

    removeFile(new File(binDir, "runData"));
    removeFile(new File(binDir, "modpack.jar"));

    deleteMods(pack.getModsDir());
    deleteMods(pack.getCoremodsDir());
    deleteMods(new File(pack.getInstalledDirectory(), "Flan"));

    // Clean old version patches so stale ones don't persist across updates
    File patchesDir = new File(pack.getInstalledDirectory(), "patches");
    if (patchesDir.isDirectory()) {
      File[] patchFiles = patchesDir.listFiles();
      if (patchFiles != null) {
        for (File patchFile : patchFiles) {
          removeFile(patchFile);
        }
      }
    }

    // First-run migration: if no extracted files manifest exists, back up config/ to a
    // timestamped folder so orphaned files from old modpack versions don't persist under
    // the new manifest tracking. Existing backups are never overwritten.
    if (!ExtractedFilesManifest.exists(binDir)) {
      File configDir = new File(pack.getInstalledDirectory(), "config");
      String[] configEntries = configDir.list();
      if (configDir.isDirectory() && configEntries != null && configEntries.length > 0) {
        File backupDir = nextAvailableBackupDir(pack.getInstalledDirectory());
        if (configDir.renameTo(backupDir)) {
          writeBackupReadme(backupDir);
          Utils.getLogger()
              .warning(
                  "No extracted files manifest found - moved config/ to "
                      + backupDir.getName()
                      + " for first-run migration");
        } else {
          Utils.getLogger()
              .warning(
                  "Failed to move config/ to "
                      + backupDir.getName()
                      + " during first-run migration");
        }
      }
    }
  }

  private static File nextAvailableBackupDir(File modpackDir) {
    String base =
        "config-backup-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
    File candidate = new File(modpackDir, base);
    int suffix = 1;
    while (candidate.exists()) {
      candidate = new File(modpackDir, base + "-" + suffix);
      suffix++;
    }
    return candidate;
  }

  private static void writeBackupReadme(File backupDir) {
    String text =
        "This folder contains config files from your previous modpack install.\n"
            + "\n"
            + "What happened:\n"
            + "Technic Launcher upgraded its file tracking on "
            + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            + ".\n"
            + "To enable orphan-file cleanup on future modpack updates, your existing\n"
            + "config/ folder was moved here.\n"
            + "\n"
            + "To restore individual files, copy them back from this folder into the\n"
            + "modpack's config/ folder.\n"
            + "\n"
            + "You can safely delete this folder once you've copied over anything you\n"
            + "want to keep.\n";
    try {
      Files.write(
          backupDir.toPath().resolve("README.txt"), text.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      Utils.getLogger().warning("Failed to write README.txt to backup folder: " + e.getMessage());
    }
  }

  private void installModpackContents(NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    List<PreparedModpackArchive> archives = prepareModpackArchives();
    if (archives.isEmpty()) {
      reporter.updateNodeProgress(100.0f);
      return;
    }

    IZipFileFilter zipFilter = new ModpackZipFilter(pack);
    final File modpackInstallDirectory = pack.getInstalledDirectory();
    int totalSteps = archives.size() * 2;
    int stepIndex = 0;

    for (PreparedModpackArchive archive : archives) {
      throwIfCancelled();
      NodeProgressReporter itemReporter = createItemReporter(reporter, stepIndex, totalSteps);
      if (!archive.cacheFile.exists() || !archive.verifier.isFileValid(archive.cacheFile)) {
        downloadFile(
            archive.mod.getUrl(),
            archive.cacheFile,
            archive.verifier,
            archive.cacheFile.getName(),
            itemReporter,
            null,
            false);
      } else {
        itemReporter.updateNodeProgress(100.0f);
      }
      stepIndex++;
      reporter.updateNodeProgress(percentage(stepIndex, totalSteps));
    }

    for (int archiveIndex = archives.size() - 1; archiveIndex >= 0; archiveIndex--) {
      throwIfCancelled();
      PreparedModpackArchive archive = archives.get(archiveIndex);
      NodeProgressReporter itemReporter = createItemReporter(reporter, stepIndex, totalSteps);

      // Detect Prism instance zips and remap paths during extraction
      PrismInstanceRemapper prismRemapper = PrismInstanceRemapper.detect(archive.cacheFile);
      if (prismRemapper != null) {
        Utils.getLogger().info("Detected Prism instance zip: " + archive.cacheFile.getName());
      }

      executeLeafTask(
          new UnzipFileTask<IMinecraftVersionInfo>(
              archive.cacheFile,
              modpackInstallDirectory,
              zipFilter,
              prismRemapper,
              allExtractedPaths),
          null,
          itemReporter);

      // Store detected Prism remapper for later runData generation
      if (prismRemapper != null) {
        if (this.detectedPrismRemapper != null) {
          Utils.getLogger()
              .warning(
                  "Multiple Prism instance zips detected - using the latest: "
                      + archive.cacheFile.getName());
        }
        this.detectedPrismRemapper = prismRemapper;
      }
      stepIndex++;
      reporter.updateNodeProgress(percentage(stepIndex, totalSteps));
    }
  }

  private void cleanupOrphanedFiles() throws IOException {
    File modpackDir = pack.getInstalledDirectory();
    File binDir = pack.getBinDir();

    // Load old manifest (from previous install)
    ExtractedFilesManifest oldManifest = ExtractedFilesManifest.load(binDir);

    // Build new manifest from the files we just extracted
    ExtractedFilesManifest newManifest =
        ExtractedFilesManifest.buildFromExtractedFiles(modpackDir, allExtractedPaths);

    // Find and delete orphans (in old but not in new)
    Set<String> orphans = ExtractedFilesManifest.findOrphans(oldManifest, newManifest);
    if (!orphans.isEmpty()) {
      int deleted = ExtractedFilesManifest.deleteOrphans(orphans, modpackDir);
      Utils.getLogger()
          .info(
              "Cleaned up " + deleted + " orphaned files out of " + orphans.size() + " candidates");
      int failed = orphans.size() - deleted;
      if (failed > 0) {
        Utils.getLogger()
            .warning(
                failed
                    + " of "
                    + orphans.size()
                    + " orphan deletion(s) failed; check earlier warnings for the affected paths");
      }
    }

    // Save new manifest
    newManifest.save(binDir);
  }

  private List<PreparedModpackArchive> prepareModpackArchives()
      throws IOException, InterruptedException {
    final File cacheDir = pack.getCacheDir();
    Set<File> processedFiles = new LinkedHashSet<>(modpackData.getMods().size());
    List<PreparedModpackArchive> archives = new ArrayList<>(modpackData.getMods().size());

    for (Mod mod : modpackData.getMods()) {
      throwIfCancelled();
      File cacheFile = mod.generateSafeCacheFile(cacheDir);

      if (!processedFiles.add(cacheFile)) {
        throw new IOException(
            "Detected overlapping files for modpack "
                + pack.getName()
                + ": "
                + cacheFile.getName());
      }

      archives.add(new PreparedModpackArchive(mod, cacheFile, createModpackVerifier(mod)));
    }

    return archives;
  }

  private IMinecraftVersionInfo resolveVersion() throws IOException, InterruptedException {
    IMinecraftVersionInfo version = versionBuilder.buildVersionFromKey(minecraftVersion);

    if (version == null && pack.isLocalOnly()) {
      throw new PackNotAvailableOfflineException(pack.getDisplayName());
    }
    if (version == null) {
      throw new DownloadException("The version.json file was invalid.");
    }

    version.setJavaRuntime(selectedJavaRuntime);
    return version;
  }

  private void writePrismRunData() throws IOException {
    if (detectedPrismRemapper == null) return;

    File runDataFile = new File(pack.getBinDir(), "runData");

    // Don't overwrite if already written by the Solder API step
    if (runDataFile.exists()) return;

    long memory = detectedPrismRemapper.getMaxMemAllocMb();
    if (memory <= 0 && patchEffectiveMinJavaMajor <= 0) return;

    pack.getBinDir().mkdirs();

    JsonObject runData = new JsonObject();
    if (patchEffectiveMinJavaMajor > 0) {
      runData.addProperty("java", String.valueOf(patchEffectiveMinJavaMajor));
    }
    if (memory > 0) {
      runData.addProperty("memory", String.valueOf(memory));
    }

    Path runDataPath = runDataFile.toPath();
    Path tmp = runDataPath.resolveSibling(runDataPath.getFileName() + ".tmp");
    try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
      MojangUtils.getGson().toJson(runData, writer);
    }
    try {
      Files.move(
          tmp, runDataPath,
          StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Utils.getLogger()
          .warning("Filesystem does not support atomic move; falling back to non-atomic replace");
      Files.move(tmp, runDataPath, StandardCopyOption.REPLACE_EXISTING);
    }

    Utils.getLogger()
        .info("Wrote Prism-derived runData: java=" + patchEffectiveMinJavaMajor + " memory=" + memory);
  }

  private void applyVersionPatches(InstallExecutionContext context) throws IOException {
    IMinecraftVersionInfo version = context.getResolvedVersion();
    File patchesDir = new File(pack.getInstalledDirectory(), "patches");

    if (!patchesDir.isDirectory()) return;

    File[] patchFiles = patchesDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (patchFiles == null || patchFiles.length == 0) return;

    // Sort by filename so the parse order is deterministic. Since patches.sort below is stable
    // (TimSort), patches with the same `order` will then apply in alphabetical filename order,
    // matching Prism's tiebreaker rule.
    Arrays.sort(patchFiles, Comparator.comparing(File::getName));

    // Parse all patches
    List<VersionPatch> patches = new ArrayList<>();
    for (File file : patchFiles) {
      try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
        VersionPatch patch = MojangUtils.getGson().fromJson(reader, VersionPatch.class);
        if (patch != null && patch.getFormatVersion() == 1) {
          patches.add(patch);
          Utils.getLogger().info("Loaded version patch: " + file.getName());
        }
      } catch (JsonParseException e) {
        Utils.getLogger()
            .warning("Failed to parse version patch " + file.getName() + ": " + e.getMessage());
      }
    }

    if (patches.isEmpty()) return;

    // Sort by order (ascending)
    patches.sort(Comparator.comparingInt(VersionPatch::getOrder));

    // Apply each patch in order
    for (VersionPatch patch : patches) {
      applyPatch(version, patch);
    }
  }

  private void applyPatch(IMinecraftVersionInfo version, VersionPatch patch) {
    // Special case: uid "net.minecraft" replaces ALL libraries.
    // This mirrors Prism where the net.minecraft component defines THE complete
    // set of base libraries. Subsequent patches add their own libs on top.
    if ("net.minecraft".equals(patch.getUid()) && patch.getLibraries() != null) {
      version.replaceAllLibraries(patch.getLibraries());
    } else if (patch.getLibraries() != null) {
      // Normal case: add libraries using Prism's merge logic.
      // If a library with the same group:artifact already exists, replace it
      // only if the new version is higher. Otherwise just add.
      for (Library lib : patch.getLibraries()) {
        mergeLibrary(version, lib);
      }
    }

    // Add JVM arguments
    if (patch.getJvmArgs() != null && !patch.getJvmArgs().isEmpty()) {
      List<Argument> args = new ArrayList<>();
      for (String arg : patch.getJvmArgs()) {
        args.add(Argument.literal(arg));
      }
      version.addJvmArguments(args);
    }

    // Add tweakers as --tweakClass game arguments
    if (patch.getTweakers() != null && !patch.getTweakers().isEmpty()) {
      List<Argument> tweakArgs = new ArrayList<>();
      for (String tweaker : patch.getTweakers()) {
        tweakArgs.add(Argument.literal("--tweakClass"));
        tweakArgs.add(Argument.literal(tweaker));
      }
      version.addGameArguments(tweakArgs);
    }

    // Override mainClass (last patch by order wins)
    if (patch.getMainClass() != null) {
      version.setMainClass(patch.getMainClass());
    }

    // Override javaVersion (last patch by order wins)
    if (patch.getJavaVersion() != null) {
      version.setMojangRuntimeInformation(patch.getJavaVersion());
    } else if (patch.getCompatibleJavaMajors() != null
        && !patch.getCompatibleJavaMajors().isEmpty()) {
      // Derive javaVersion from compatibleJavaMajors (Prism format)
      VersionJavaInfo derived =
          deriveJavaVersionFromCompatibleMajors(patch.getCompatibleJavaMajors());
      if (derived != null) {
        version.setMojangRuntimeInformation(derived);
      }
      // Track minimum Java version for runData (take the highest minimum across all patches)
      int minMajor =
          patch.getCompatibleJavaMajors().stream().mapToInt(Integer::intValue).min().orElse(0);
      if (minMajor > patchEffectiveMinJavaMajor) {
        patchEffectiveMinJavaMajor = minMajor;
      }
    }
  }

  private VersionJavaInfo deriveJavaVersionFromCompatibleMajors(List<Integer> majors) {
    int highest = majors.stream().mapToInt(Integer::intValue).max().orElse(0);

    // Primary: query Mojang's live JRE manifest for the best available component.
    VersionJavaInfo dynamic = deriveJavaVersionFromAvailableComponents(highest);
    if (dynamic != null) return dynamic;

    // Fallback: hardcoded mapping for offline / manifest-failure cases.
    return deriveJavaVersionFromCompatibleMajorsHardcoded(highest);
  }

  /**
   * Picks the best Mojang JRE component for the requested major version by querying the live JRE
   * manifest. Returns the component whose major is the highest one that is ≤ {@code requestedMajor}.
   * Returns null on any failure so the caller can fall back to a hardcoded mapping.
   *
   * <p>Major versions are parsed from {@link
   * net.technicpack.minecraftcore.mojang.java.JavaRuntimeInfo#getName()} via {@link
   * JavaVersionComparator#getMajor(String)}; components whose version string isn't recognized are
   * skipped rather than failing the whole lookup.
   */
  private VersionJavaInfo deriveJavaVersionFromAvailableComponents(int requestedMajor) {
    try {
      JavaRuntimesIndex index =
          MojangUtils.getJavaRuntimesIndex(fileSystem.getRuntimesDirectory().resolve("_index.json"));
      if (index == null) return null;
      Map<String, List<JavaRuntime>> available = index.getRuntimesForCurrentOS();
      if (available == null || available.isEmpty()) return null;

      JavaVersionComparator comparator = new JavaVersionComparator();
      String bestComponent = null;
      int bestMajor = -1;
      for (Map.Entry<String, List<JavaRuntime>> entry : available.entrySet()) {
        List<JavaRuntime> runtimes = entry.getValue();
        if (runtimes == null || runtimes.isEmpty()) continue;
        String versionName = runtimes.get(0).getVersion().getName();
        int major;
        try {
          major = comparator.getMajor(versionName);
        } catch (IllegalArgumentException ex) {
          Utils.getLogger()
              .log(
                  Level.FINE,
                  "Skipping JRE component "
                      + entry.getKey()
                      + " with unrecognized version "
                      + versionName,
                  ex);
          continue;
        }
        if (major > requestedMajor || major <= bestMajor) continue;
        bestMajor = major;
        bestComponent = entry.getKey();
      }
      if (bestComponent != null) return new VersionJavaInfo(bestComponent, bestMajor);
    } catch (RuntimeException ex) {
      Utils.getLogger()
          .log(
              Level.WARNING,
              "JRE manifest lookup failed; falling back to hardcoded component mapping",
              ex);
    }
    return null;
  }

  /**
   * Hardcoded fallback used only when the live JRE manifest can't be fetched/parsed. Updated as
   * Mojang ships new components: alpha=16, gamma=17, delta=21, epsilon=25; jre-legacy=8 covers
   * everything below 16. To add a new component (e.g. java-runtime-zeta for Java 29+), insert a new
   * branch above {@code highest >= 25}. Note that {@link #deriveJavaVersionFromAvailableComponents}
   * already handles new components automatically when the network is available — this list only
   * matters offline.
   */
  private static VersionJavaInfo deriveJavaVersionFromCompatibleMajorsHardcoded(int highest) {
    String component;
    int majorVersion;
    if (highest >= 25) {
      component = "java-runtime-epsilon";
      majorVersion = 25;
    } else if (highest >= 21) {
      component = "java-runtime-delta";
      majorVersion = 21;
    } else if (highest >= 17) {
      component = "java-runtime-gamma";
      majorVersion = 17;
    } else if (highest >= 16) {
      component = "java-runtime-alpha";
      majorVersion = 16;
    } else {
      component = "jre-legacy";
      majorVersion = 8;
    }

    return new VersionJavaInfo(component, majorVersion);
  }

  /**
   * Merge a library into the version using Prism's logic: find existing by
   * group:artifact:classifier, replace if new version is higher, otherwise add. Classifier is part
   * of the match key because variants like {@code natives-windows} and {@code natives-linux} are
   * distinct artifacts on disk and must not displace each other.
   */
  private static void mergeLibrary(IMinecraftVersionInfo version, Library newLib) {
    String newGroup = newLib.getGradleGroup();
    String newArtifact = newLib.getGradleArtifact();
    String newClassifier = newLib.getGradleClassifier();

    for (Library existing : version.getLibraries()) {
      if (newGroup.equals(existing.getGradleGroup())
          && newArtifact.equals(existing.getGradleArtifact())
          && Objects.equals(newClassifier, existing.getGradleClassifier())) {
        // Found matching group:artifact:classifier - compare versions
        ComparableVersion newVersion = new ComparableVersion(newLib.getGradleVersion());
        ComparableVersion existingVersion = new ComparableVersion(existing.getGradleVersion());

        if (newVersion.compareTo(existingVersion) > 0) {
          // New version is higher - remove old, add new
          version.removeLibrary(existing.getName());
          version.addLibrary(newLib);
        }
        // If new version is not higher, keep existing (do nothing)
        return;
      }
    }

    // No existing library with this group:artifact:classifier - just add
    version.addLibrary(newLib);
  }

  private void prepareResolvedVersion(InstallExecutionContext context) throws IOException {
    IMinecraftVersionInfo version = context.getResolvedVersion();
    List<Library> librariesToInstall = new ArrayList<>();
    LinkedHashMap<InstallLibraryKey, Library> dedupedLibraries = new LinkedHashMap<>();

    boolean isLegacy = MojangUtils.isLegacyVersion(version.getParentVersion());
    if (isLegacy) {
      Library legacyWrapper =
          new Library(
              "net.technicpack:legacywrapper:1.2.1",
              TechnicConstants.TECHNIC_LIB_REPO
                  + "net/technicpack/legacywrapper/1.2.1/legacywrapper-1.2.1.jar",
              "741cbc946421a5a59188a51108e1ce5cb5674681",
              77327);
      version.addLibrary(legacyWrapper);
      version.setMainClass("net.technicpack.legacywrapper.Launch");
    }

    final boolean hasNeoForge = MojangUtils.hasNeoForge(version);
    final boolean hasModernMinecraftForge = MojangUtils.hasModernMinecraftForge(version);

    if (hasModernMinecraftForge || hasNeoForge) {
      final String[] versionIdParts = version.getId().split("-", 3);
      final boolean is1_12_2 = versionIdParts[0].equals("1.12.2");

      if (is1_12_2) {
        for (Library library : version.getLibrariesForCurrentOS(settings, selectedJavaRuntime)) {
          if (library.getGradleGroup().equals("net.minecraftforge")
              && library.getGradleArtifact().equals("forge")
              && (library.getGradleClassifier() == null
                  || library.getGradleClassifier().isEmpty())) {
            String oldName = library.getName();
            library.setName(library.getName() + ":universal");
            Downloads downloads = library.getDownloads();
            Artifact artifact = downloads.getArtifact();
            artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());
            library.setName(oldName);
            break;
          }
        }
      }

      for (Library library : readInstallerLibraries()) {
        if (library.isMinecraftForge() && is1_12_2) {
          continue;
        }

        if (library.getGradleGroup().equals("net.minecraftforge")
            && library.getGradleArtifact().equals("forge")
            && library.getGradleClassifier() != null
            && library.getGradleClassifier().equals("universal")
            && !is1_12_2) {
          Downloads downloads = library.getDownloads();
          Artifact artifact = downloads.getArtifact();
          if (artifact.getUrl() == null || artifact.getUrl().isEmpty()) {
            artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());
          }
        }

        putLibraryIfAbsent(dedupedLibraries, library);
      }

      if (!is1_12_2) {
        Library forgeWrapper =
            new Library(
                "io.github.zekerzhayard:ForgeWrapper:1.6.0-technic",
                TechnicConstants.TECHNIC_LIB_REPO
                    + "io/github/zekerzhayard/ForgeWrapper/1.6.0-technic/ForgeWrapper-1.6.0-technic.jar",
                "8764cbf4c7ded7ac0ad9136a0070bbfeee8813cf",
                34944);
        version.prependLibrary(forgeWrapper);
        version.setMainClass("io.github.zekerzhayard.forgewrapper.installer.Main");

        for (Library library : version.getLibrariesForCurrentOS(settings, selectedJavaRuntime)) {
          if (library.getGradleGroup().equals("net.minecraftforge")
              && library.getGradleArtifact().equals("forge")
              && (library.getGradleClassifier() == null
                  || library.getGradleClassifier().isEmpty())) {
            String oldName = library.getName();
            library.setName(library.getName() + ":launcher");
            Downloads downloads = library.getDownloads();
            Artifact artifact = downloads.getArtifact();
            if (artifact.getUrl() == null || artifact.getUrl().isEmpty()) {
              artifact.setUrl("https://maven.minecraftforge.net/" + library.getArtifactPath());
            }
            library.setName(oldName);
            break;
          }

          if (library.getGradleGroup().equals("net.minecraftforge")
              && library.getGradleArtifact().equals("forge")
              && library.getGradleClassifier() != null
              && library.getGradleClassifier().equals("client")) {
            version.removeLibrary(library.getName());
          }
        }
      }
    }

    for (Library library : version.getLibrariesForCurrentOS(settings, selectedJavaRuntime)) {
      // Remove the Forge library if not using modern Forge AND modpack.jar exists
      // (since modpack.jar provides Forge classes on the classpath).
      // If modpack.jar doesn't exist, keep the Forge library so patches can provide it.
      if (library.isMinecraftForge()
          && !hasModernMinecraftForge
          && new File(pack.getBinDir(), "modpack.jar").exists()) {
        version.removeLibrary(library.getName());
        continue;
      }

      if (isLegacy && library.getName().startsWith("net.minecraft:launchwrapper:")) {
        version.removeLibrary(library.getName());
        continue;
      }

      if (library.isLog4j()
          && !library.getGradleVersion().equals("2.0-beta9-fixed")
          && (new ComparableVersion(library.getGradleVersion()))
                  .compareTo(new ComparableVersion("2.16.0"))
              < 0) {
        Library fixedLog4j = createPatchedLog4j(library);
        version.addLibrary(fixedLog4j);
        putLibraryIfAbsent(dedupedLibraries, fixedLog4j);
        version.removeLibrary(library.getName());
        continue;
      }

      putLibraryIfAbsent(dedupedLibraries, library);
    }

    librariesToInstall.addAll(dedupedLibraries.values());
    context.setLibrariesToInstall(librariesToInstall);
  }

  private void installFmlLibraries(Map<String, String> fmlLibs, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    Path fmlLibsCache = fileSystem.getCacheDirectory().resolve("fmllibs");
    Files.createDirectories(fmlLibsCache);
    File modpackFmlLibDir = new File(pack.getInstalledDirectory(), "lib");

    int index = 0;
    for (Map.Entry<String, String> entry : fmlLibs.entrySet()) {
      throwIfCancelled();
      String name = entry.getKey();
      String sha1 = entry.getValue();
      SHA1FileVerifier verifier = sha1.isEmpty() ? null : new SHA1FileVerifier(sha1);
      File cached = fmlLibsCache.resolve(name).toFile();
      File target = new File(modpackFmlLibDir, name);

      NodeProgressReporter itemReporter = createItemReporter(reporter, index, fmlLibs.size());
      if (!target.exists() || (verifier != null && !verifier.isFileValid(target))) {
        downloadFile(
            TechnicConstants.TECHNIC_FML_LIB_REPO + name,
            cached,
            verifier,
            name,
            itemReporter,
            null,
            false);
        executeLeafTask(
            new CopyFileTask<IMinecraftVersionInfo>(cached, target), null, itemReporter);
      }

      index++;
      reporter.updateNodeProgress(percentage(index, fmlLibs.size()));
    }
  }

  private void installVersionLibraries(
      InstallExecutionContext context, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    List<Library> libraries = context.getLibrariesToInstall();
    if (libraries.isEmpty()) {
      reporter.updateNodeProgress(100.0f);
      return;
    }

    int index = 0;
    for (Library library : libraries) {
      throwIfCancelled();
      NodeProgressReporter itemReporter = createItemReporter(reporter, index, libraries.size());
      installVersionLibrary(context, library, itemReporter);
      index++;
      reporter.updateNodeProgress(percentage(index, libraries.size()));
    }
  }

  private void installVersionLibrary(
      InstallExecutionContext context, Library library, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    // Local libraries (MMC-hint: local) live in the modpack's libraries/ directory
    if (library.isLocal()) {
      Path localPath = library.resolveLocalPath(pack.getInstalledDirectory().toPath());
      if (localPath != null) {
        return;
      }
      throw new IOException(
          "Local library "
              + library.getName()
              + " not found in "
              + pack.getInstalledDirectory().toPath().resolve("libraries"));
    }

    String nativeClassifier =
        library.resolveNativeClassifier(
            OperatingSystem.getOperatingSystem().getName(),
            context.getResolvedVersion().getJavaRuntime().getOsArch());
    File extractDirectory = nativeClassifier != null ? new File(pack.getBinDir(), "natives") : null;

    final String bitness = context.getResolvedVersion().getJavaRuntime().getBitness();
    String path = library.getArtifactPath(nativeClassifier).replace("${arch}", bitness);
    Path cache = fileSystem.getCacheDirectory().resolve(path);
    if (cache.getParent() != null) {
      Files.createDirectories(cache.getParent());
    }

    String sha1 = library.getArtifactSha1(nativeClassifier);
    IFileVerifier verifier =
        (sha1 != null && !sha1.isEmpty()) ? new SHA1FileVerifier(sha1) : new ValidZipFileVerifier();

    boolean cacheValid = Files.isRegularFile(cache) && verifier.isFileValid(cache);
    String url = null;
    if (!cacheValid) {
      url = library.getDownloadUrl(path).replace("${arch}", bitness);
      if (sha1 == null || sha1.isEmpty()) {
        String md5 = Utils.getETag(url);
        if (md5 != null && !md5.isEmpty()) {
          verifier = new MD5FileVerifier(md5);
        }
      }

      downloadFile(url, cache.toFile(), verifier, library.getName(), reporter, null, false);
    }

    if (extractDirectory != null) {
      IZipFileFilter filter = null;
      if (library.getExtract() != null) {
        filter = new ExtractRulesFileFilter(library.getExtract());
      }
      executeLeafTask(
          new UnzipFileTask<IMinecraftVersionInfo>(cache.toFile(), extractDirectory, filter),
          context.getResolvedVersion(),
          reporter);
    }
  }

  private void installAssets(InstallExecutionContext context, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    IMinecraftVersionInfo version = context.getResolvedVersion();
    AssetIndex assetIndex = version.getAssetIndex();
    if (assetIndex == null) {
      throw new RuntimeException("No asset index detected, cannot continue");
    }

    File assetsDirectory = fileSystem.getAssetsDirectory().toFile();
    File assetsFile =
        new File(assetsDirectory + File.separator + "indexes", assetIndex.getId() + ".json");
    File parent = assetsFile.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }

    IFileVerifier indexVerifier =
        assetIndex.getSha1() != null
            ? new SHA1FileVerifier(assetIndex.getSha1())
            : new ValidJsonFileVerifier(MojangUtils.getGson());

    if (!assetsFile.exists() || !indexVerifier.isFileValid(assetsFile)) {
      downloadFile(
          assetIndex.getUrl(),
          assetsFile,
          indexVerifier,
          assetIndex.getId() + ".json",
          reporter,
          null,
          false);
    }

    JsonObject obj = readAssetsIndex(assetsFile.toPath());
    boolean isVirtual =
        obj.has(VIRTUAL_FIELD)
            && obj.get(VIRTUAL_FIELD).isJsonPrimitive()
            && obj.get(VIRTUAL_FIELD).getAsBoolean();
    boolean mapToResources =
        obj.has(MAP_TO_RESOURCES_FIELD)
            && obj.get(MAP_TO_RESOURCES_FIELD).isJsonPrimitive()
            && obj.get(MAP_TO_RESOURCES_FIELD).getAsBoolean();

    version.setAreAssetsVirtual(isVirtual);
    version.setAssetsMapToResources(mapToResources);

    JsonObject allAssets = obj.getAsJsonObject(OBJECTS_FIELD);
    if (allAssets == null) {
      throw new DownloadException("The assets json file was invalid.");
    }

    String assetsKey = version.getAssetsKey();
    if (assetsKey == null || assetsKey.isEmpty()) {
      assetsKey = "legacy";
    }

    List<Map.Entry<String, JsonElement>> assets = new ArrayList<>(allAssets.entrySet());
    int index = 0;
    for (Map.Entry<String, JsonElement> assetObj : assets) {
      throwIfCancelled();
      NodeProgressReporter itemReporter = createItemReporter(reporter, index, assets.size());
      processAsset(
          assetObj,
          assetsKey,
          isVirtual,
          mapToResources,
          assetsDirectory.getAbsolutePath(),
          itemReporter);
      index++;
      reporter.updateNodeProgress(percentage(index, assets.size()));
    }
  }

  private void processAsset(
      Map.Entry<String, JsonElement> assetObj,
      String assetsKey,
      boolean isVirtual,
      boolean mapToResources,
      String assetsDirectory,
      NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    String assetPath = assetObj.getKey();
    JsonObject assetData = assetObj.getValue().getAsJsonObject();
    String hash = assetData.get(HASH_FIELD).getAsString();
    long size = assetData.get(SIZE_FIELD).getAsLong();

    if (hash == null || hash.isEmpty()) {
      throw new DownloadException(String.format("No hash provided for %s", assetPath));
    }

    IFileVerifier verifier =
        hash.length() == 40 ? new SHA1FileVerifier(hash) : new FileSizeVerifier(size);
    File target =
        new File(String.format("%s/objects/%s", assetsDirectory, hash.substring(0, 2)), hash);
    Files.createDirectories(target.getParentFile().toPath());
    downloadFile(
        MojangUtils.getResourceUrl(hash), target, verifier, assetPath, reporter, null, false);

    File cloneTo = null;
    if (isVirtual) {
      cloneTo = new File(String.format("%s/virtual/%s/%s", assetsDirectory, assetsKey, assetPath));
    } else if (mapToResources) {
      cloneTo = new File(pack.getResourcesDir(), assetPath);
    }

    if (cloneTo != null && !cloneTo.exists()) {
      Files.createDirectories(cloneTo.getParentFile().toPath());
      executeLeafTask(new CopyFileTask<IMinecraftVersionInfo>(target, cloneTo), null, reporter);
    }
  }

  private void installJavaRuntime(InstallExecutionContext context, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    IMinecraftVersionInfo version = context.getResolvedVersion();
    VersionJavaInfo runtimeInfo = version.getMojangRuntimeInformation();
    if (runtimeInfo == null) {
      reporter.updateNodeProgress(100.0f);
      return;
    }

    final String runtimeName = runtimeInfo.getComponent();
    JavaRuntimesIndex availableRuntimes =
        MojangUtils.getJavaRuntimesIndex(fileSystem.getRuntimesDirectory().resolve("_index.json"));
    if (availableRuntimes == null) {
      throw new DownloadException("Failed to get Mojang JRE information");
    }

    JavaRuntime manifestInfo = availableRuntimes.getRuntimeForCurrentOS(runtimeName);
    if (manifestInfo == null) {
      throw new DownloadException("A Mojang JRE is not available for the current OS");
    }

    Download runtimeDownload = manifestInfo.getManifest();
    Path manifestPath =
        fileSystem.getRuntimesDirectory().resolve("manifests").resolve(runtimeName + ".json");
    Files.createDirectories(manifestPath.getParent());
    downloadFile(
        runtimeDownload.getUrl(),
        manifestPath.toFile(),
        new SHA1FileVerifier(runtimeDownload.getSha1()),
        runtimeName + ".json",
        reporter,
        null,
        false);

    JavaRuntimeManifest manifest = readJavaRuntimeManifest(manifestPath);
    Path runtimeRoot = fileSystem.getRuntimesDirectory().resolve(runtimeInfo.getComponent());
    ensurePathIsSafe(fileSystem.getRuntimesDirectory(), runtimeRoot);
    Files.createDirectories(runtimeRoot);

    processJavaDirectories(manifest, runtimeRoot);

    List<Map.Entry<String, JavaRuntimeFile>> fileEntries =
        collectJavaEntries(manifest, JavaRuntimeFileType.FILE);
    List<Map.Entry<String, JavaRuntimeFile>> linkEntries =
        collectJavaEntries(manifest, JavaRuntimeFileType.LINK);
    int totalEntries = fileEntries.size() + linkEntries.size();

    int index = 0;
    for (Map.Entry<String, JavaRuntimeFile> entry : fileEntries) {
      throwIfCancelled();
      NodeProgressReporter itemReporter = createItemReporter(reporter, index, totalEntries);
      processJavaEntry(runtimeRoot, entry, context.getResolvedVersion(), itemReporter);
      index++;
      reporter.updateNodeProgress(percentage(index, totalEntries));
    }

    for (Map.Entry<String, JavaRuntimeFile> entry : linkEntries) {
      throwIfCancelled();
      NodeProgressReporter itemReporter = createItemReporter(reporter, index, totalEntries);
      processJavaEntry(runtimeRoot, entry, context.getResolvedVersion(), itemReporter);
      index++;
      reporter.updateNodeProgress(percentage(index, totalEntries));
    }

    version.setJavaRuntime(getJavaRuntime(runtimeRoot));
  }

  private void processJavaDirectories(JavaRuntimeManifest manifest, Path runtimeRoot)
      throws IOException {
    for (Map.Entry<String, JavaRuntimeFile> entry : manifest.getFiles().entrySet()) {
      if (entry.getValue().getType() != JavaRuntimeFileType.DIRECTORY) {
        continue;
      }

      Path dir = runtimeRoot.resolve(entry.getKey());
      ensurePathIsSafe(runtimeRoot, dir);
      Files.createDirectories(dir);
    }
  }

  private List<Map.Entry<String, JavaRuntimeFile>> collectJavaEntries(
      JavaRuntimeManifest manifest, JavaRuntimeFileType type) {
    return manifest.getFiles().entrySet().stream()
        .filter(entry -> entry.getValue().getType() == type)
        .collect(Collectors.toList());
  }

  private void processJavaEntry(
      Path runtimeRoot,
      Map.Entry<String, JavaRuntimeFile> entry,
      IMinecraftVersionInfo version,
      NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    String path = entry.getKey();
    JavaRuntimeFile runtimeFile = entry.getValue();

    if (runtimeFile.getType() == JavaRuntimeFileType.FILE) {
      Path target = runtimeRoot.resolve(path);
      ensurePathIsSafe(runtimeRoot, target);
      Files.createDirectories(target.getParent());

      Download rawDownload = runtimeFile.getDownloads().getRaw();
      Download lzmaDownload = runtimeFile.getDownloads().getLzma();
      IFileVerifier verifier = new SHA1FileVerifier(rawDownload.getSha1());
      boolean useLzma =
          lzmaDownload != null
              && !lzmaDownload.getUrl().isEmpty()
              && ((double) lzmaDownload.getSize() / rawDownload.getSize() <= 0.66);

      String url = useLzma ? lzmaDownload.getUrl() : rawDownload.getUrl();
      String decompressor = useLzma ? CompressorStreamFactory.LZMA : null;

      downloadFile(
          url,
          target.toFile(),
          verifier,
          target.getFileName().toString(),
          reporter,
          decompressor,
          runtimeFile.isExecutable());
      return;
    }

    if (runtimeFile.getType() == JavaRuntimeFileType.LINK) {
      Path link = runtimeRoot.resolve(path);
      ensurePathIsSafe(runtimeRoot, link);
      Path target = link.resolve(runtimeFile.getTarget());
      ensurePathIsSafe(runtimeRoot, target);
      executeLeafTask(
          new EnsureLinkedFileTask<IMinecraftVersionInfo>(link, target), version, reporter);
    }
  }

  private static Library createPatchedLog4j(Library library) {
    final String[] libNameParts = library.getName().split(":");
    String log4jVersion = libNameParts[2].equals("2.0-beta9") ? "2.0-beta9-fixed" : "2.16.0";
    String artifactName = libNameParts[1];

    String sha1;
    int size;
    if ("2.16.0".equals(log4jVersion)) {
      switch (artifactName) {
        case "log4j-api":
          sha1 = "f821a18687126c2e2f227038f540e7953ad2cc8c";
          size = 301892;
          break;
        case "log4j-core":
          sha1 = "539a445388aee52108700f26d9644989e7916e7c";
          size = 1789565;
          break;
        case "log4j-slf4j18-impl":
          sha1 = "0c880a059056df5725f5d8d1035276d9749eba6d";
          size = 21249;
          break;
        default:
          throw new RuntimeException(
              "Unknown log4j artifact " + artifactName + ", cannot continue");
      }
    } else {
      switch (artifactName) {
        case "log4j-api":
          sha1 = "b61eaf2e64d8b0277e188262a8b771bbfa1502b3";
          size = 107347;
          break;
        case "log4j-core":
          sha1 = "677991ea2d7426f76309a73739cecf609679492c";
          size = 677588;
          break;
        default:
          throw new RuntimeException(
              "Unknown log4j artifact " + artifactName + ", cannot continue");
      }
    }

    String url =
        String.format(
            TechnicConstants.TECHNIC_LIB_REPO + "org/apache/logging/log4j/%1$s/%2$s/%1$s-%2$s.jar",
            artifactName,
            log4jVersion);
    return new Library(
        "org.apache.logging.log4j:" + artifactName + ":" + log4jVersion, url, sha1, size);
  }

  private List<Library> readInstallerLibraries() throws IOException {
    try (JarFile modpackJar = new JarFile(new File(pack.getBinDir(), "modpack.jar"))) {
      JarEntry entry = modpackJar.getJarEntry("install_profile.json");
      if (entry == null) {
        throw new RuntimeException("modpack.jar does not contain the install_profile.json file");
      }

      try (InputStream inputStream = modpackJar.getInputStream(entry);
          InputStreamReader reader = new InputStreamReader(inputStream)) {
        JsonElement root = JsonParser.parseReader(reader);
        JsonObject rootObj = root.getAsJsonObject();
        JsonElement librariesElement = rootObj.get("libraries");

        if (librariesElement == null || !librariesElement.isJsonArray()) {
          throw new RuntimeException("install_profile.json does not contain libraries");
        }

        List<Library> libraries =
            MojangUtils.getGson()
                .fromJson(librariesElement, new TypeToken<List<Library>>() {}.getType());
        if (libraries == null) {
          return Collections.emptyList();
        }

        LinkedHashMap<InstallLibraryKey, Library> deduped = new LinkedHashMap<>();
        for (Library library : libraries) {
          putLibraryIfAbsent(deduped, library);
        }
        return new ArrayList<>(deduped.values());
      }
    }
  }

  private static void putLibraryIfAbsent(
      Map<InstallLibraryKey, Library> dedupedLibraries, Library library) {
    dedupedLibraries.putIfAbsent(InstallLibraryKey.from(library), library);
  }

  private JsonObject readAssetsIndex(Path assetsIndex) throws IOException {
    try (Reader reader = Files.newBufferedReader(assetsIndex, StandardCharsets.UTF_8)) {
      JsonObject jsonObject = MojangUtils.getGson().fromJson(reader, JsonObject.class);
      if (jsonObject == null) {
        throw new DownloadException(String.format("The assets file %s is invalid", assetsIndex));
      }
      return jsonObject;
    } catch (JsonParseException e) {
      throw new IOException(String.format("Failed to load assets index file %s", assetsIndex), e);
    }
  }

  private JavaRuntimeManifest readJavaRuntimeManifest(Path runtimeManifestFile) throws IOException {
    try (Reader reader = Files.newBufferedReader(runtimeManifestFile, StandardCharsets.UTF_8)) {
      JavaRuntimeManifest manifest =
          MojangUtils.getGson().fromJson(reader, JavaRuntimeManifest.class);
      if (manifest == null) {
        throw new DownloadException("The Java runtime manifest is invalid.");
      }
      return manifest;
    } catch (JsonParseException e) {
      throw new IOException("Failed to parse Java runtime manifest", e);
    }
  }

  private void downloadFile(
      String url,
      File destination,
      IFileVerifier verifier,
      String description,
      NodeProgressReporter reporter,
      String decompressor,
      boolean executable)
      throws IOException, InterruptedException {
    throwIfCancelled();
    if (destination.exists() && (verifier == null || verifier.isFileValid(destination))) {
      reporter.updateNodeProgress(100.0f);
      return;
    }

    DownloadFilePlanAction<Void> action =
        new DownloadFilePlanAction<>(url, destination, verifier, description);
    if (decompressor != null) {
      action.withDecompressor(decompressor);
    }
    if (executable) {
      action.withExecutable();
    }
    action.execute(null, reporter);
    throwIfCancelled();
  }

  private void executeLeafTask(
      net.technicpack.launchercore.install.tasks.IInstallTask<IMinecraftVersionInfo> task,
      IMinecraftVersionInfo metadata,
      NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    throwIfCancelled();
    InstallExecutionContext context = new InstallExecutionContext();
    context.setResolvedVersion(metadata);
    new LegacyTaskPlanAction<InstallExecutionContext, IMinecraftVersionInfo>(
            task, InstallExecutionContext::getResolvedVersion)
        .execute(context, reporter);
    throwIfCancelled();
  }

  private void throwIfCancelled() throws InterruptedException {
    throwIfCancelled(cancellationCheck);
  }

  static void throwIfCancelled(BooleanSupplier cancellationCheck) throws InterruptedException {
    if (Thread.currentThread().isInterrupted() || cancellationCheck.getAsBoolean()) {
      throw new InterruptedException();
    }
  }

  static void throwIfInterrupted() throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }
  }

  private static NodeProgressReporter createItemReporter(
      NodeProgressReporter reporter, int index, int total) {
    final float start = percentage(index, total);
    final float end = percentage(index + 1, total);

    return new NodeProgressReporter() {
      @Override
      public void updateNodeProgress(float percent) {
        reporter.updateNodeProgress(start + ((end - start) * clamp(percent) / 100.0f));
      }

      @Override
      public void updateCurrentItem(
          String label, net.technicpack.launchercore.progress.CurrentItemMode mode, Float percent) {
        reporter.updateCurrentItem(label, mode, percent);
      }
    };
  }

  private static float percentage(int completed, int total) {
    if (total <= 0) {
      return 100.0f;
    }
    return ((float) completed / (float) total) * 100.0f;
  }

  private static float clamp(float percent) {
    if (percent < 0.0f) {
      return 0.0f;
    }
    if (percent > 100.0f) {
      return 100.0f;
    }
    return percent;
  }

  private static IFileVerifier createModpackVerifier(Mod mod) {
    String md5 = mod.getMd5();
    if (md5 != null && !md5.isEmpty()) {
      return new MD5FileVerifier(md5);
    }
    return new ValidZipFileVerifier();
  }

  private void deleteMods(File modsDir) throws CacheDeleteException {
    if (modsDir == null || !modsDir.exists() || !modsDir.isDirectory()) {
      return;
    }

    File[] mods = modsDir.listFiles();
    if (mods == null) {
      return;
    }

    for (File mod : mods) {
      if (mod.isDirectory()) {
        deleteMods(mod);
        continue;
      }

      if (mod.getName().endsWith(".zip")
          || mod.getName().endsWith(".jar")
          || mod.getName().endsWith(".litemod")) {
        removeFile(mod);
      }
    }
  }

  private void removeFile(File file) throws CacheDeleteException {
    if (file.exists()) {
      try {
        Files.delete(file.toPath());
      } catch (IOException e) {
        throw new CacheDeleteException(file.getAbsolutePath(), e);
      }
    }
  }

  private void ensurePathIsSafe(Path root, Path target) {
    Path normalizedRoot = root.normalize();
    Path normalizedTarget = target.normalize();
    if (!normalizedTarget.startsWith(normalizedRoot)) {
      throw new SecurityException(
          String.format("JRE entry attempted to be placed outside of JRE root folder: %s", target));
    }
  }

  private static @NotNull IJavaRuntime getJavaRuntime(Path runtimeRoot) {
    final OperatingSystem os = OperatingSystem.getOperatingSystem();
    final Path runtimeExecutable;

    if (os == OperatingSystem.WINDOWS) {
      runtimeExecutable = runtimeRoot.resolve("bin/javaw.exe");
    } else if (os == OperatingSystem.OSX) {
      runtimeExecutable = runtimeRoot.resolve("jre.bundle/Contents/Home/bin/java");
    } else {
      runtimeExecutable = runtimeRoot.resolve("bin/java");
    }

    return new net.technicpack.launchercore.launch.java.version.FileBasedJavaRuntime(
        runtimeExecutable);
  }

  static class InstallExecutionContext {
    private IMinecraftVersionInfo resolvedVersion;
    private List<Library> librariesToInstall = Collections.emptyList();

    IMinecraftVersionInfo getResolvedVersion() {
      return resolvedVersion;
    }

    void setResolvedVersion(IMinecraftVersionInfo resolvedVersion) {
      this.resolvedVersion = resolvedVersion;
    }

    List<Library> getLibrariesToInstall() {
      return librariesToInstall;
    }

    void setLibrariesToInstall(List<Library> librariesToInstall) {
      this.librariesToInstall =
          librariesToInstall == null ? Collections.<Library>emptyList() : librariesToInstall;
    }
  }

  private static final class PreparedModpackArchive {
    private final Mod mod;
    private final File cacheFile;
    private final IFileVerifier verifier;

    private PreparedModpackArchive(Mod mod, File cacheFile, IFileVerifier verifier) {
      this.mod = mod;
      this.cacheFile = cacheFile;
      this.verifier = verifier;
    }
  }

  private static final class InstallLibraryKey {
    private final String normalizedName;
    private final List<Rule> rules;
    private final Map<String, String> natives;
    private final ExtractRules extract;

    private InstallLibraryKey(
        String normalizedName,
        List<Rule> rules,
        Map<String, String> natives,
        ExtractRules extract) {
      this.normalizedName = normalizedName;
      this.rules = rules;
      this.natives = natives;
      this.extract = extract;
    }

    private static InstallLibraryKey from(Library library) {
      return new InstallLibraryKey(
          library.getNormalizedName(),
          library.getRules(),
          library.getNatives(),
          library.getExtract());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InstallLibraryKey that = (InstallLibraryKey) o;
      return Objects.equals(normalizedName, that.normalizedName)
          && Objects.equals(rules, that.rules)
          && Objects.equals(natives, that.natives)
          && Objects.equals(extract, that.extract);
    }

    @Override
    public int hashCode() {
      return Objects.hash(normalizedName, rules, natives, extract);
    }
  }
}
