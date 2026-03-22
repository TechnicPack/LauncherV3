package net.technicpack.autoupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.Objects;
import java.util.logging.Level;
import net.technicpack.autoupdate.io.LauncherResource;
import net.technicpack.autoupdate.io.StreamVersion;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.PlanBuilder;
import net.technicpack.launchercore.install.plan.actions.DownloadFilePlanAction;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA256FileVerifier;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.utilslib.Utils;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class UpdatePlanner {
  private static final String DOWNLOADS_PHASE = "downloads";
  private static final String ACTIONS_PHASE = "actions";

  private final LauncherFileSystem fileSystem;

  public UpdatePlanner(LauncherFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  public ExecutionPlan<UpdateContext> planUpdater(
      IUpdateStream updateStream, UpdatePlanRequest request) throws IOException {
    Objects.requireNonNull(updateStream, "Update stream must not be null");
    Objects.requireNonNull(request, "Update plan request must not be null");

    StreamVersion version = fetchVersion(updateStream, request.getStreamName());
    PlanBuilder<UpdateContext> builder = new PlanBuilder<>();
    builder.addPhase(DOWNLOADS_PHASE, request.getDownloadsPhaseLabel());

    if (version == null || version.getBuild() == 0) {
      return builder.build();
    }

    for (LauncherResource resource : version.getResources()) {
      IFileVerifier verifier = createFileVerifier(resource);
      String resourceDescription = request.getLauncherAssetDescription(resource.getFilename());

      File targetFile =
          fileSystem.getLauncherAssetsDirectory().resolve(resource.getFilename()).toFile();

      if (targetFile.exists() && verifier != null && verifier.isFileValid(targetFile)) {
        continue;
      }

      builder.addNode(
          getResourceNodeId(resource),
          DOWNLOADS_PHASE,
          resourceDescription,
          1.0f,
          createResourceDownloadAction(resource, targetFile, verifier, resourceDescription));
    }

    if (request.isSkipUpdate() || version.getBuild() == request.getCurrentBuild()) {
      return builder.build();
    }

    if (request.getRunningPath() == null) {
      throw new DownloadException(
          "Could not load a running path for currently-executing launcher.");
    }

    String updateUrl =
        request.getRunningPath().endsWith(".exe") ? version.getExeUrl() : version.getJarUrl();

    builder.addNode(
        "download-launcher-update",
        DOWNLOADS_PHASE,
        request.getLauncherUpdateDescription(),
        1.0f,
        (context, reporter) -> {
          DownloadFilePlanAction<UpdateContext> action =
              new DownloadFilePlanAction<UpdateContext>(
                      updateUrl,
                      request.getTempLauncher().toFile(),
                      null,
                      request.getLauncherUpdateDescription())
                  .withTaskDescriptionAsProgressLabel();
          action.execute(context, reporter);
          context.getRelauncher().setUpdated();
        });

    if (!request.isUpdateOnly()) {
      builder.addPhase(ACTIONS_PHASE, request.getLaunchMoverDescription());
      builder.addNode(
          "launch-mover",
          ACTIONS_PHASE,
          request.getLaunchMoverDescription(),
          0.0f,
          java.util.Collections.singletonList("download-launcher-update"),
          (context, reporter) ->
              context
                  .getRelauncher()
                  .launch(
                      request.getTempLauncher().toAbsolutePath().toString(),
                      context.getRelauncher().buildMoverArgs()));
    }

    return builder.build();
  }

  public ExecutionPlan<UpdateContext> planMover(MoverPlanRequest request) {
    PlanBuilder<UpdateContext> builder = new PlanBuilder<>();
    builder.addPhase(ACTIONS_PHASE, request.getMoveDescription());
    builder.addNode(
        "copy-launcher-package",
        ACTIONS_PHASE,
        request.getMoveDescription(),
        1.0f,
        (context, reporter) -> context.getRelauncher().copyToMoveTarget(request.getMoveTarget()));
    builder.addNode(
        "launch-launcher-mode",
        ACTIONS_PHASE,
        request.getFinalLaunchDescription(),
        0.0f,
        java.util.Collections.singletonList("copy-launcher-package"),
        (context, reporter) ->
            context
                .getRelauncher()
                .launch(
                    request.getMoveTarget().toAbsolutePath().toString(),
                    context.getRelauncher().buildLauncherArgs(request.isLegacyMover())));
    return builder.build();
  }

  private static StreamVersion fetchVersion(IUpdateStream updateStream, String streamName) {
    try {
      return updateStream.getStreamVersion(streamName);
    } catch (RestfulAPIException e) {
      Utils.getLogger().log(Level.SEVERE, "Failed to query update stream", e);
      return null;
    }
  }

  private static DownloadFilePlanAction<UpdateContext> createResourceDownloadAction(
      LauncherResource resource, File targetFile, IFileVerifier verifier, String taskDescription)
      throws DownloadException {
    String zstdUrl = resource.getZstdUrl();
    if (zstdUrl != null && !zstdUrl.isEmpty()) {
      return new DownloadFilePlanAction<UpdateContext>(
              zstdUrl, targetFile, verifier, taskDescription)
          .withDecompressor(CompressorStreamFactory.ZSTANDARD)
          .withTaskDescriptionAsProgressLabel();
    }

    return new DownloadFilePlanAction<UpdateContext>(
            resource.getUrl(), targetFile, verifier, taskDescription)
        .withTaskDescriptionAsProgressLabel();
  }

  private static IFileVerifier createFileVerifier(LauncherResource resource) {
    if (resource.getSha256() != null && !resource.getSha256().isEmpty()) {
      return new SHA256FileVerifier(resource.getSha256());
    }
    if (resource.getMd5() != null && !resource.getMd5().isEmpty()) {
      return new MD5FileVerifier(resource.getMd5());
    }
    return null;
  }

  private static String getResourceNodeId(LauncherResource resource) {
    return "download-resource-" + resource.getFilename();
  }

  public static class UpdateContext {
    private final Relauncher relauncher;

    public UpdateContext(Relauncher relauncher) {
      this.relauncher = relauncher;
    }

    public Relauncher getRelauncher() {
      return relauncher;
    }
  }

  public static class UpdatePlanRequest {
    private final String streamName;
    private final int currentBuild;
    private final boolean skipUpdate;
    private final boolean updateOnly;
    private final String runningPath;
    private final Path tempLauncher;
    private final String downloadsPhaseLabel;
    private final String launchMoverDescription;
    private final String launcherAssetDescriptionTemplate;
    private final String launcherUpdateDescription;

    public UpdatePlanRequest(
        String streamName,
        int currentBuild,
        boolean skipUpdate,
        boolean updateOnly,
        String runningPath,
        Path tempLauncher,
        String downloadsPhaseLabel,
        String launchMoverDescription,
        String launcherAssetDescriptionTemplate,
        String launcherUpdateDescription) {
      this.streamName = streamName;
      this.currentBuild = currentBuild;
      this.skipUpdate = skipUpdate;
      this.updateOnly = updateOnly;
      this.runningPath = runningPath;
      this.tempLauncher = tempLauncher;
      this.downloadsPhaseLabel = downloadsPhaseLabel;
      this.launchMoverDescription = launchMoverDescription;
      this.launcherAssetDescriptionTemplate = launcherAssetDescriptionTemplate;
      this.launcherUpdateDescription = launcherUpdateDescription;
    }

    public String getStreamName() {
      return streamName;
    }

    public int getCurrentBuild() {
      return currentBuild;
    }

    public boolean isSkipUpdate() {
      return skipUpdate;
    }

    public boolean isUpdateOnly() {
      return updateOnly;
    }

    public String getRunningPath() {
      return runningPath;
    }

    public Path getTempLauncher() {
      return tempLauncher;
    }

    public String getDownloadsPhaseLabel() {
      return downloadsPhaseLabel;
    }

    public String getLaunchMoverDescription() {
      return launchMoverDescription;
    }

    public String getLauncherAssetDescription(String filename) {
      if (launcherAssetDescriptionTemplate == null
          || launcherAssetDescriptionTemplate.trim().isEmpty()) {
        return "Launcher Asset: " + filename;
      }

      try {
        if (launcherAssetDescriptionTemplate.contains("%s")) {
          return String.format(launcherAssetDescriptionTemplate, filename);
        }
      } catch (IllegalFormatException ignored) {
        // Fallback to sane output if localization format tokens are malformed.
      }

      return launcherAssetDescriptionTemplate + " " + filename;
    }

    public String getLauncherUpdateDescription() {
      if (launcherUpdateDescription == null || launcherUpdateDescription.trim().isEmpty()) {
        return "Launcher Update";
      }
      return launcherUpdateDescription;
    }
  }

  public static class MoverPlanRequest {
    private final Path moveTarget;
    private final boolean legacyMover;
    private final String moveDescription;
    private final String finalLaunchDescription;

    public MoverPlanRequest(
        Path moveTarget,
        boolean legacyMover,
        String moveDescription,
        String finalLaunchDescription) {
      this.moveTarget = moveTarget;
      this.legacyMover = legacyMover;
      this.moveDescription = moveDescription;
      this.finalLaunchDescription = finalLaunchDescription;
    }

    public Path getMoveTarget() {
      return moveTarget;
    }

    public boolean isLegacyMover() {
      return legacyMover;
    }

    public String getMoveDescription() {
      return moveDescription;
    }

    public String getFinalLaunchDescription() {
      return finalLaunchDescription;
    }
  }
}
