package net.technicpack.launchercore.install.tasks;

import com.google.gson.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.rest.io.Modpack;
import net.technicpack.utilslib.Utils;

public class WriteRundataFile implements IInstallTask<IMinecraftVersionInfo> {
  private ModpackModel modpackModel;
  private Modpack modpack;

  public WriteRundataFile(ModpackModel modpackModel, Modpack modpack) {
    this.modpackModel = modpackModel;
    this.modpack = modpack;
  }

  @Override
  public String getTaskDescription() {
    return "Writing Runtime Data";
  }

  @Override
  public float getTaskProgress() {
    return 0;
  }

  @Override
  public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException {
    if ((modpack.getJava() == null || modpack.getJava().isEmpty())
        && (modpack.getMemory() == null || modpack.getMemory().isEmpty())) return;

    File file = modpackModel.getBinDir();
    File runDataFile = new File(file, "runData");
    JsonObject runData = new JsonObject();
    JsonElement java = getJsonValue(modpack.getJava());
    JsonElement memory = getJsonValue(modpack.getMemory());
    runData.add("java", java);
    runData.add("memory", memory);

    Path runDataPath = runDataFile.toPath();
    Path tmp = runDataPath.resolveSibling(runDataPath.getFileName() + ".tmp");
    try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
      Utils.getGson().toJson(runData, writer);
    } catch (JsonIOException e) {
      throw new IOException(String.format("Error writing runData file %s", runDataFile), e);
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
  }

  private JsonElement getJsonValue(String value) {
    if (value == null) return JsonNull.INSTANCE;
    return new JsonPrimitive(value);
  }
}
