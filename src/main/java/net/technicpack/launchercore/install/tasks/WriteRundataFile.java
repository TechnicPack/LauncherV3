package net.technicpack.launchercore.install.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.rest.io.Modpack;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class WriteRundataFile implements IInstallTask {
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
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        if ((modpack.getJava() == null || modpack.getJava().isEmpty()) && (modpack.getMemory() == null || modpack.getMemory().isEmpty()))
            return;

        File file = modpackModel.getBinDir();
        File runDataFile = new File(file, "runData");
        JsonObject runData = new JsonObject();
        JsonElement java = getJsonValue(modpack.getJava());
        JsonElement memory = getJsonValue(modpack.getMemory());
        runData.add("java", java);
        runData.add("memory", memory);
        String output = Utils.getGson().toJson(runData);
        FileUtils.writeStringToFile(runDataFile, output);
    }

    private JsonElement getJsonValue(String value) {
        if (value == null)
            return JsonNull.INSTANCE;
        return new JsonPrimitive(value);
    }
}
