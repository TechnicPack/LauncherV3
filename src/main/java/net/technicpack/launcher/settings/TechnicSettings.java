/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.settings;

import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Level;

public class TechnicSettings {
    public static final String STABLE = "stable";
    public static final String BETA = "beta";

    private transient File settingsFile;
    private transient File technicRoot;
    private int memory;
    private LaunchAction launchAction = LaunchAction.HIDE;
    private String buildStream = STABLE;
    private boolean showConsole;
    private String languageCode = "default";
    private String clientId = UUID.randomUUID().toString();
    private String directory;
    private String javaArgs;
    private int latestNewsArticle;
    private boolean launchToModpacks;
    private String javaVersion = "default";
    /**
     * 64 bit if true, 32 bit if false
     */
    private boolean javaBitness = true;

    private String launcherSettingsVersion = "0";

    public File getFilePath() { return this.settingsFile; }
    public void setFilePath(File settingsFile) {
        this.settingsFile = settingsFile;
    }

    public File getTechnicRoot() {
        if (technicRoot == null || !technicRoot.exists())
            buildTechnicRoot();

        return technicRoot;
    }

    public String getLauncherSettingsVersion() { return launcherSettingsVersion; }
    public void setLauncherSettingsVersion(String version) { this.launcherSettingsVersion = version; }

    public boolean isPortable() {
        return (directory != null && !directory.isEmpty() && directory.equalsIgnoreCase("portable"));
    }

    public void setPortable() {
        directory = "portable";
    }

    public void installTo(String directory) {
        this.directory = directory;
    }

    public int getMemory() { return memory; }
    public void setMemory(int memory) {
        this.memory = memory;
        save();
    }

    public LaunchAction getLaunchAction() { return launchAction; }
    public void setLaunchAction(LaunchAction launchAction) {
        this.launchAction = launchAction;
        save();
    }

    public String getBuildStream() { return buildStream; }
    public void setBuildStream(String buildStream) {
        this.buildStream = buildStream;
        save();
    }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
        save();
    }

    public boolean getJavaBitness() { return javaBitness; }
    public void setJavaBitness(boolean javaBitness) {
        this.javaBitness = javaBitness;
        save();
    }

    public boolean getShowConsole() { return showConsole; }
    public void setShowConsole(boolean showConsole) {
        this.showConsole = showConsole;
        save();
    }

    //Whether to launch into the modpacks tab directly or launch to the discover tab
    public boolean getLaunchToModpacks() { return launchToModpacks; }
    public void setLaunchToModpacks(boolean launchToModpacks) {
        this.launchToModpacks = launchToModpacks;
        save();
    }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
        save();
    }

    public int getLatestNewsArticle() { return latestNewsArticle; }
    public void setLatestNewsArticle(int latestNewsArticle)
    {
        this.latestNewsArticle = latestNewsArticle;
        save();
    }

    public String getClientId() { return clientId; }

    public String getJavaArgs() { return javaArgs; }
    public void setJavaArgs(String args) { javaArgs = args; }

    public void save() {
        String json = Utils.getGson().toJson(this);

        try {
            FileUtils.writeStringToFile(settingsFile, json, Charset.forName("UTF-8"));
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Unable to save installed " + settingsFile);
        }
    }

    protected void buildTechnicRoot() {
        if (directory == null || directory.isEmpty() || directory.equalsIgnoreCase("portable"))
            technicRoot = settingsFile.getParentFile();
        else
            technicRoot = new File(directory);

        if (!technicRoot.exists())
            technicRoot.mkdirs();
    }


}
