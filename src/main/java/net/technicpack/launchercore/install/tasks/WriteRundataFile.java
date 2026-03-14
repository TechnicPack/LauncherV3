package net.technicpack.launchercore.install.tasks;

import com.google.gson.*;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.rest.io.Modpack;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WriteRundataFile implements IInstallTask<IMinecraftVersionInfo> {
    private final ModpackModel modpackModel;
    private final Modpack modpack;
    private final boolean forceWrite;

    public WriteRundataFile(ModpackModel modpackModel, Modpack modpack) {
        this(modpackModel, modpack, true);
    }

    public WriteRundataFile(ModpackModel modpackModel, Modpack modpack, boolean forceWrite) {
        this.modpackModel = modpackModel;
        this.modpack = modpack;
        this.forceWrite = forceWrite;
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
        if ((modpack.getJava() == null || modpack.getJava().isEmpty()) && (modpack.getMemory() == null || modpack.getMemory().isEmpty()))
            return;

        if (!forceWrite) {
            File runDataFile = new File(modpackModel.getBinDir(), "runData");
            if (runDataFile.exists())
                return;
            if (modpackModel.isLocalOnly())
                return;
        }

        File file = modpackModel.getBinDir();
        File runDataFile = new File(file, "runData");
        JsonObject runData = new JsonObject();
        JsonElement java = getJsonValue(modpack.getJava());
        JsonElement memory = getJsonValue(modpack.getMemory());
        runData.add("java", java);
        runData.add("memory", memory);
        try (Writer writer = Files.newBufferedWriter(runDataFile.toPath(), StandardCharsets.UTF_8)) {
            Utils.getGson().toJson(runData, writer);
        } catch (JsonIOException e) {
            throw new IOException(String.format("Error writing runData file %s", runDataFile), e);
        }
    }

    private JsonElement getJsonValue(String value) {
        if (value == null)
            return JsonNull.INSTANCE;
        return new JsonPrimitive(value);
    }
}
