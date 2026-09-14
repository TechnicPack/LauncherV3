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

package net.technicpack.launchercore.modpacks.packinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.solder.io.SolderPackInfo;

/**
 * One resolved Platform/Solder snapshot. Platform owns identity and presentation; the Solder
 * selected by that Platform response owns the build catalog and installation data. Missing Solder
 * data means unavailable builds, never a fallback to Platform's unrelated ZIP/version fields.
 */
public final class CombinedPackInfo implements PackInfo {
  private final PlatformPackInfo platformPackInfo;
  private final SolderPackInfo solderPackInfo;

  public CombinedPackInfo(PlatformPackInfo platformPackInfo, SolderPackInfo solderPackInfo) {
    this.platformPackInfo = Objects.requireNonNull(platformPackInfo, "platformPackInfo");
    if (!platformPackInfo.hasSolder()) {
      throw new IllegalArgumentException("Combined pack info requires a Platform Solder endpoint");
    }
    String name = platformPackInfo.getName();
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Combined pack info requires a pack name");
    }
    if (solderPackInfo != null && !name.equals(solderPackInfo.getName())) {
      throw new IllegalArgumentException("Platform and Solder pack identities differ");
    }
    this.solderPackInfo = solderPackInfo;
  }

  @Override
  public String getName() {
    return platformPackInfo.getName();
  }

  @Override
  public String getDisplayName() {
    return platformPackInfo.getDisplayName();
  }

  @Override
  public String getWebSite() {
    return platformPackInfo.getWebSite();
  }

  @Override
  public String getDiscordId() {
    return platformPackInfo.getDiscordId();
  }

  @Override
  public Resource getIcon() {
    return platformPackInfo.getIcon();
  }

  @Override
  public Resource getBackground() {
    return platformPackInfo.getBackground();
  }

  @Override
  public Resource getLogo() {
    return platformPackInfo.getLogo();
  }

  @Override
  public String getRecommended() {
    return solderPackInfo == null ? null : solderPackInfo.getRecommended();
  }

  @Override
  public String getLatest() {
    return solderPackInfo == null ? null : solderPackInfo.getLatest();
  }

  @Override
  public List<String> getBuilds() {
    return solderPackInfo == null ? Collections.emptyList() : solderPackInfo.getBuilds();
  }

  @Override
  public ArrayList<FeedItem> getFeed() {
    return platformPackInfo.getFeed();
  }

  @Override
  public String getDescription() {
    return platformPackInfo.getDescription();
  }

  @Override
  public Integer getRuns() {
    return platformPackInfo.getRuns();
  }

  @Override
  public Integer getInstalls() {
    return platformPackInfo.getInstalls();
  }

  @Override
  public Integer getLikes() {
    return platformPackInfo.getLikes();
  }

  @Override
  public boolean isServerPack() {
    return platformPackInfo.isServerPack();
  }

  @Override
  public boolean isOfficial() {
    return platformPackInfo.isOfficial();
  }

  @Override
  public Modpack getModpack(String build) throws BuildInaccessibleException {
    if (solderPackInfo == null) {
      throw new BuildInaccessibleException(
          getDisplayName(),
          build,
          new IllegalStateException("Solder pack metadata is unavailable"));
    }
    return solderPackInfo.getModpack(build);
  }

  @Override
  public boolean isComplete() {
    // Resolution has finished even when Solder is unavailable. Discovery must not overwrite it.
    return true;
  }

  @Override
  public boolean isLocal() {
    return solderPackInfo == null || solderPackInfo.isLocal();
  }

  @Override
  public boolean hasSolder() {
    return true;
  }
}
