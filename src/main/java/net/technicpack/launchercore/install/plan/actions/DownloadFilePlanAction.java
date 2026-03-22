package net.technicpack.launchercore.install.plan.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.install.plan.PlanNodeAction;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.utilslib.Utils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamProvider;

public class DownloadFilePlanAction<TContext> implements PlanNodeAction<TContext> {
  private static final Set<String> AVAILABLE_DECOMPRESSORS;

  static {
    SortedMap<String, CompressorStreamProvider> availableProviders =
        CompressorStreamFactory.findAvailableCompressorInputStreamProviders();

    AVAILABLE_DECOMPRESSORS =
        availableProviders.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
  }

  private final String url;
  private final File destination;
  private final String taskDescription;
  private final IFileVerifier fileVerifier;
  private boolean executable;
  private String decompressor;
  private boolean useTaskDescriptionForProgressLabel;

  public DownloadFilePlanAction(
      String url, File destination, IFileVerifier verifier, String taskDescription) {
    this.url = url;
    this.destination = destination;
    this.fileVerifier = verifier;
    this.taskDescription = taskDescription;
  }

  @Override
  public void execute(TContext context, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    final boolean needsDecompression = decompressor != null;

    IFileVerifier downloadFileVerifier;
    File tempDestination;

    if (needsDecompression) {
      downloadFileVerifier = null;
      tempDestination = new File(this.destination + ".temp");
    } else {
      downloadFileVerifier = fileVerifier;
      tempDestination = this.destination;
    }

    reporter.updateCurrentItem(taskDescription, CurrentItemMode.INDETERMINATE, null);

    Utils.downloadFile(
        url,
        tempDestination.getName(),
        tempDestination.getAbsolutePath(),
        null,
        downloadFileVerifier,
        new DownloadProgressAdapter(reporter, taskDescription, useTaskDescriptionForProgressLabel));

    if (needsDecompression) {
      decompress(tempDestination);
    }

    if (!this.destination.exists()) {
      throw new DownloadException("Failed to download " + this.destination.getName() + ".");
    }

    if (this.executable && !this.destination.setExecutable(true)) {
      throw new DownloadException("Failed to set " + this.destination.getName() + " as executable");
    }

    reporter.updateNodeProgress(100.0f);
  }

  static String formatProgressLabel(String label) {
    if (label == null || label.trim().isEmpty()) {
      return "Downloading...";
    }

    String normalized = label.trim();
    String lowerCase = normalized.toLowerCase();
    if (lowerCase.startsWith("downloading ")
        || lowerCase.startsWith("extracting ")
        || lowerCase.startsWith("processing ")
        || lowerCase.startsWith("download failed")) {
      return normalized;
    }

    return "Downloading " + normalized;
  }

  static String buildProgressLabel(
      String fileName, String taskDescription, boolean useTaskDescriptionForProgressLabel) {
    if (useTaskDescriptionForProgressLabel) {
      if (taskDescription != null && !taskDescription.trim().isEmpty()) {
        return taskDescription.trim();
      }
      return formatProgressLabel(fileName);
    }

    return formatProgressLabel(fileName);
  }

  public DownloadFilePlanAction<TContext> withExecutable() {
    this.executable = true;
    return this;
  }

  public DownloadFilePlanAction<TContext> withTaskDescriptionAsProgressLabel() {
    this.useTaskDescriptionForProgressLabel = true;
    return this;
  }

  public DownloadFilePlanAction<TContext> withDecompressor(String decompressor)
      throws DownloadException {
    if (!AVAILABLE_DECOMPRESSORS.contains(decompressor.toLowerCase())) {
      throw new DownloadException(
          String.format("Decompressor '%s' is not available", decompressor));
    }

    this.decompressor = decompressor;
    return this;
  }

  private void decompress(File tempDestination) throws IOException {
    Utils.getLogger()
        .fine("Decompressing " + tempDestination.getAbsolutePath() + " using " + decompressor);

    try (FileInputStream fis = new FileInputStream(tempDestination);
        CompressorInputStream cis =
            new CompressorStreamFactory().createCompressorInputStream(decompressor, fis);
        FileOutputStream fos = new FileOutputStream(this.destination)) {
      byte[] buffer = new byte[65536];
      int n;
      while ((n = cis.read(buffer)) != -1) {
        fos.write(buffer, 0, n);
      }
    } catch (CompressorException e) {
      throw new DownloadException("Failed to decompress " + tempDestination.getName(), e);
    }

    if (this.fileVerifier.isFileValid(this.destination)) {
      try {
        Files.delete(tempDestination.toPath());
      } catch (IOException e) {
        throw new DownloadException(
            "Failed to delete temporary file " + tempDestination.getAbsolutePath(), e);
      }
    } else {
      try {
        Files.delete(this.destination.toPath());
      } catch (IOException e) {
        throw new DownloadException(
            "Failed to delete broken downloaded file " + this.destination.getAbsolutePath(), e);
      }
      throw new DownloadException("Failed to download " + this.destination.getAbsolutePath());
    }
  }

  private static class DownloadProgressAdapter implements DownloadListener {
    private final NodeProgressReporter reporter;
    private final String taskDescription;
    private final boolean useTaskDescriptionForProgressLabel;

    private DownloadProgressAdapter(
        NodeProgressReporter reporter,
        String taskDescription,
        boolean useTaskDescriptionForProgressLabel) {
      this.reporter = reporter;
      this.taskDescription = taskDescription;
      this.useTaskDescriptionForProgressLabel = useTaskDescriptionForProgressLabel;
    }

    @Override
    public void stateChanged(String fileName, float progress) {
      String progressLabel =
          buildProgressLabel(fileName, taskDescription, useTaskDescriptionForProgressLabel);
      if (progress > 0.0f) {
        reporter.updateCurrentItem(progressLabel, CurrentItemMode.DETERMINATE, progress);
        reporter.updateNodeProgress(progress);
      } else {
        reporter.updateCurrentItem(progressLabel, CurrentItemMode.INDETERMINATE, null);
      }
    }
  }
}
