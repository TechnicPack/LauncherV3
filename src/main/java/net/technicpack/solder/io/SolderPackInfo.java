/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.solder.io;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.solder.ISolderPackApi;

@SuppressWarnings({"unused"})
public class SolderPackInfo extends RestObject implements PackInfo {

  private String name;

  @SerializedName("display_name")
  private String displayName;

  private String recommended;
  private String latest;
  private List<String> builds;
  private transient ISolderPackApi solder;
  private transient boolean isLocal = false;

  private SolderPackInfo() {
    // Empty constructor for GSON
  }

  public SolderPackInfo(SolderPackInfo other) {
    name = other.name;
    displayName = other.displayName;
    recommended = other.recommended;
    latest = other.latest;
    builds = other.builds == null ? null : new ArrayList<>(other.builds);
    solder = other.solder;
    isLocal = other.isLocal;
  }

  public void validate(String expectedName) throws RestfulAPIException {
    if (hasError()) {
      throw new RestfulAPIException("Error in Solder response: " + getError());
    }
    if (expectedName == null
        || expectedName.trim().isEmpty()
        || name == null
        || name.trim().isEmpty()
        || !expectedName.equals(name)) {
      throw new RestfulAPIException("Invalid Solder pack identity for " + expectedName);
    }
    if (builds == null) {
      throw new RestfulAPIException("Missing Solder build catalog for " + expectedName);
    }
    for (String build : builds) {
      if (build == null || build.trim().isEmpty()) {
        throw new RestfulAPIException("Invalid Solder build catalog for " + expectedName);
      }
    }
  }

  public ISolderPackApi getSolder() {
    return solder;
  }

  public void setSolder(ISolderPackApi solder) {
    this.solder = solder;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getWebSite() {
    return null;
  }

  @Override
  public Resource getIcon() {
    return null;
  }

  @Override
  public Resource getBackground() {
    return null;
  }

  @Override
  public Resource getLogo() {
    return null;
  }

  @Override
  public String getRecommended() {
    return recommended;
  }

  @Override
  public String getLatest() {
    return latest;
  }

  @Override
  public List<String> getBuilds() {
    return builds == null ? Collections.emptyList() : builds;
  }

  @Override
  public String getDiscordId() {
    return null;
  }

  @Override
  public ArrayList<FeedItem> getFeed() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Integer getLikes() {
    return null;
  }

  @Override
  public Integer getInstalls() {
    return null;
  }

  @Override
  public Integer getRuns() {
    return null;
  }

  @Override
  public boolean isServerPack() {
    return false;
  }

  @Override
  public boolean isOfficial() {
    return false;
  }

  @Override
  public Modpack getModpack(String build) throws BuildInaccessibleException {
    if (solder == null) {
      throw new BuildInaccessibleException(name, build);
    }
    Modpack modpack = solder.getPackBuild(build);
    if (modpack == null) {
      throw new BuildInaccessibleException(name, build);
    }
    return modpack;
  }

  @Override
  public boolean isComplete() {
    return false;
  }

  @Override
  public boolean isLocal() {
    return isLocal || getBuilds().isEmpty();
  }

  public void setLocal() {
    isLocal = true;
  }

  @Override
  public boolean hasSolder() {
    return true;
  }

  @Override
  public String toString() {
    return "SolderPackInfo{"
        + "name='"
        + name
        + '\''
        + ", display_name='"
        + displayName
        + '\''
        + ", recommended='"
        + recommended
        + '\''
        + ", latest='"
        + latest
        + '\''
        + ", builds="
        + builds
        + ", solder="
        + solder
        + '}';
  }
}
