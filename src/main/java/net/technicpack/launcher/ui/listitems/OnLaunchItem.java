package net.technicpack.launcher.ui.listitems;

import net.technicpack.launchercore.util.LaunchAction;

public class OnLaunchItem {
    private String text;
    private LaunchAction launchAction;

    public OnLaunchItem(String text, LaunchAction action) {
        this.text = text;
        this.launchAction = action;
    }

    public LaunchAction getLaunchAction() { return launchAction; }
    public String toString() { return text; }
}
