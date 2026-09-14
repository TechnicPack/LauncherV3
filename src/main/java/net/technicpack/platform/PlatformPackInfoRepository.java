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

package net.technicpack.platform;

import java.util.logging.Level;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.packinfo.CombinedPackInfo;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;

public class PlatformPackInfoRepository implements IAuthoritativePackSource {
  private final IPlatformApi platform;
  private final ISolderApi solder;

  public PlatformPackInfoRepository(IPlatformApi platform, ISolderApi solder) {
    this.platform = platform;
    this.solder = solder;
  }

  @Override
  public PackInfo getPackInfo(InstalledPack pack) {
    return getPlatformPackInfo(pack.getName());
  }

  @Override
  public PackInfo getCompletePackInfo(PackInfo pack) {
    return getPlatformPackInfo(pack.getName());
  }

  protected PackInfo getPlatformPackInfo(String slug) {
    try {
      return resolve(slug, platform.getPlatformPackInfoForBulk(slug), true, false);
    } catch (RestfulAPIException e) {
      Utils.getLogger().log(Level.WARNING, "Unable to load platform pack " + slug, e);
      return null;
    }
  }

  /** Resolves a fresh snapshot using the same ownership rules as bulk discovery. */
  public PackInfo refreshPackInfo(String slug, boolean invalidateSolderCache)
      throws RestfulAPIException {
    return resolve(slug, platform.getPlatformPackInfo(slug), false, invalidateSolderCache);
  }

  private PackInfo resolve(
      String slug, PlatformPackInfo platformInfo, boolean bulk, boolean invalidateSolderCache)
      throws RestfulAPIException {
    if (platformInfo == null) return null;
    platformInfo.validate(slug);
    if (!platformInfo.hasSolder()) return platformInfo;

    SolderPackInfo solderInfo = null;
    try {
      ISolderPackApi solderPack =
          solder.getSolderPack(
              platformInfo.getSolder(), slug, solder.getMirrorUrl(platformInfo.getSolder()));
      if (invalidateSolderCache) solderPack.invalidateCache();
      SolderPackInfo candidate = bulk ? solderPack.getPackInfoForBulk() : solderPack.getPackInfo();
      if (candidate != null) {
        candidate.validate(slug);
        solderInfo = candidate;
      }
    } catch (RestfulAPIException e) {
      Utils.getLogger().log(Level.SEVERE, "Failed to query Solder for modpack " + slug, e);
    }
    return new CombinedPackInfo(platformInfo, solderInfo);
  }
}
