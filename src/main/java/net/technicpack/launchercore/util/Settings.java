/*
 * This file is part of Technic Launcher.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.util;

import com.google.gson.JsonSyntaxException;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Level;

public class Settings {
	public static final String STABLE = "stable";
	public static final String BETA = "beta";
	private String directory;
	private int memory;
	private LaunchAction launchAction;
	private String buildStream = STABLE;
	private boolean showConsole;
	private String languageCode = "default";
	private String clientId = UUID.randomUUID().toString();

    private transient File loadedFromFile;

	public static Settings load(File settings) {
		if (settings == null || !settings.exists()) {
			Utils.getLogger().log(Level.WARNING, "Unable to load settings from " + settings + " because it does not exist.");
			return null;
		}

		try {
			String json = FileUtils.readFileToString(settings, Charset.forName("UTF-8"));
			Settings settingsObj = Utils.getGson().fromJson(json, Settings.class);

            if (settingsObj == null) {
                Utils.getLogger().log(Level.WARNING, "Unable to load settings from "+settings+" because the JSON file couldn't be parsed.");
                return null;
            }

            settingsObj.setLoadedFromFile(settings);
            return settingsObj;
		}  catch (JsonSyntaxException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to load settings from " + settings);
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to load settings from " + settings);
		}

        return null;
	}

    public File getLoadedFromFile() { return loadedFromFile; }
    public void setLoadedFromFile(File loadedFrom) { loadedFromFile = loadedFrom; }

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
		save();
	}

    public void save() {
        save(loadedFromFile);
    }

	public void save(File settings) {
        if (settings == null) {
            Utils.getLogger().log(Level.SEVERE, "Unable to save settings- the target file is unknown.");
        }

		String json = Utils.getGson().toJson(this);
        loadedFromFile = settings;

		try {
			FileUtils.writeStringToFile(settings, json, Charset.forName("UTF-8"));
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Unable to save settings " + settings, e);
		}
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
		save();
	}

	public LaunchAction getLaunchAction() {
		return launchAction;
	}

	public void setLaunchAction(LaunchAction launchAction) {
	    this.launchAction = launchAction;
	    save();
	}

	public String getBuildStream() {
		return buildStream;
	}

	public void setBuildStream(String buildStream) {
		this.buildStream = buildStream;
		save();
	}
	
	public String getClientId() {
		return clientId;
	}

	public boolean getShowConsole() {
		return showConsole;
	}

	public void setShowConsole(boolean showConsole) {
		this.showConsole = showConsole;
		save();
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
		save();
	}

	@Override
	public String toString() {
		return "Settings{" +
				"directory='" + directory + '\'' +
				", memory=" + memory +
				", buildStream='" + buildStream + '\'' +
				", showConsole=" + showConsole +
				", launchAction='" + launchAction +'\'' +
				", languageCode='" + languageCode + '\'' +
				'}';
	}
}
