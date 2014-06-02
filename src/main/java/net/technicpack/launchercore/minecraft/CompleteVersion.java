/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.minecraft;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CompleteVersion implements Version {

    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    private String minecraftArguments;
    private String javaArguments;
    private List<Library> libraries;
    private String mainClass;
    private int minimumLauncherVersion;
    private String incompatibilityReason;
    private List<Rule> rules;
    private String assets;
    private transient boolean areAssetsVirtual;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ReleaseType getType() {
        return type;
    }

    @Override
    public void setType(ReleaseType type) {
        this.type = type;
    }

    @Override
    public Date getUpdatedTime() {
        return time;
    }

    @Override
    public void setUpdatedTime(Date updatedTime) {
        this.time = updatedTime;
    }

    @Override
    public Date getReleaseTime() {
        return releaseTime;
    }

    @Override
    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public String getMinecraftArguments() {
        return minecraftArguments;
    }

    public String getJavaArguments() {
        return javaArguments;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public List<Library> getLibrariesForOS() {
        List<Library> libraryList = new ArrayList<Library>(libraries.size());
        for (Library library : libraries) {
            if (library.isForCurrentOS()) {
                libraryList.add(library);
            }
        }
        return libraryList;
    }

    public String getMainClass() {
        return mainClass;
    }

    public int getMinimumLauncherVersion() {
        return minimumLauncherVersion;
    }

    public String getIncompatibilityReason() {
        return incompatibilityReason;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public String getAssetsKey() {
        return assets;
    }

    public boolean getAreAssetsVirtual() {
        return areAssetsVirtual;
    }

    public void setAreAssetsVirtual(boolean areAssetsVirtual) {
        this.areAssetsVirtual = areAssetsVirtual;
    }

    @Override
    public String toString() {
        return "CompleteVersion{" +
                "id='" + id + '\'' +
                ", time=" + time +
                ", releaseTime=" + releaseTime +
                ", type=" + type +
                ", minecraftArguments='" + minecraftArguments + '\'' +
                ", libraries=" + libraries +
                ", mainClass='" + mainClass + '\'' +
                ", minimumLauncherVersion=" + minimumLauncherVersion +
                ", incompatibilityReason='" + incompatibilityReason + '\'' +
                ", rules=" + rules +
                '}';
    }
}
