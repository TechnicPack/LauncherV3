package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.rest.io.Modpack;

import java.io.File;
import java.io.IOException;

public class CheckRunDataFile implements IInstallTask<MojangVersion> {
    private final ModpackModel modpackModel;
    private final Modpack modpack;
    private final TaskGroup<MojangVersion> writeRunDataGroup;

    public CheckRunDataFile(ModpackModel modpackModel, Modpack modpack, TaskGroup<MojangVersion> writeRunDataGroup) {
        this.modpackModel = modpackModel;
        this.modpack = modpack;
        this.writeRunDataGroup = writeRunDataGroup;
    }

    @Override
    public String getTaskDescription() {
        return "Checking Runtime Data...";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<MojangVersion> queue) {
        File file = modpackModel.getBinDir();
        File runDataFile = new File(file, "runData");

        if (runDataFile.exists())
            return;
        if (modpackModel.isLocalOnly())
            return;

        writeRunDataGroup.addTask(new WriteRundataFile(modpackModel, modpack));
    }
}
