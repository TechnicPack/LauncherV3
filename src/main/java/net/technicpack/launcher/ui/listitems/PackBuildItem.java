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

package net.technicpack.launcher.ui.listitems;

import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.ui.lang.ResourceLoader;

public class PackBuildItem {
    private boolean isRecommended;
    private boolean isLatest;
    private String buildNumber;
    private String display;

    public PackBuildItem(String buildNumber, ResourceLoader loader, ModpackModel model) {
        this.buildNumber = buildNumber;
        this.isRecommended = buildNumber.equals(model.getRecommendedBuild());
        this.isLatest = buildNumber.equals(model.getLatestBuild());

        display = buildNumber;

        if (isRecommended)
            display = display + " - " + loader.getString("modpackoptions.build.recommended");
        else if (isLatest)
            display = display + " - " + loader.getString("modpackoptions.build.latest");
    }

    public String getBuildNumber() { return buildNumber; }
    public boolean isRecommended() { return isRecommended; }
    public boolean isLatest() { return isLatest; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != this.getClass())
            return false;
        PackBuildItem item = (PackBuildItem)obj;

        return item.getBuildNumber().equals(buildNumber);
    }

    @Override
    public int hashCode() {
        return getBuildNumber().hashCode();
    }

    @Override
    public String toString() {
        return display;
    }
}
