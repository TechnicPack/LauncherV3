package net.technicpack.autoupdate.tasks;

import net.technicpack.autoupdate.Relauncher;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;

import java.io.File;

public class LaunchMoverMode implements IInstallTask<Void> {

    private String description;
    private Relauncher relauncher;
    private File tempLauncher;

    public LaunchMoverMode(String description, File tempLauncher, Relauncher relauncher) {
        this.relauncher = relauncher;
        this.tempLauncher = tempLauncher;
        this.description = description;
    }

    @Override
    public String getTaskDescription() {
        return description;
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<Void> queue) {
        relauncher.launch(tempLauncher.getAbsolutePath(), relauncher.buildMoverArgs());
    }
}
