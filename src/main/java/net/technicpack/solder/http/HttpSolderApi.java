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

package net.technicpack.solder.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderClientIdProvider;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.FullModpacks;
import net.technicpack.solder.io.Solder;
import net.technicpack.solder.io.SolderPackInfo;

public class HttpSolderApi implements ISolderApi {
  private final ISolderClientIdProvider clientIdProvider;
  private final Map<String, String> mirrorUrls = new HashMap<>();

  public HttpSolderApi(ISolderClientIdProvider clientIdProvider) {
    this.clientIdProvider = clientIdProvider;
  }

  @Override
  public ISolderPackApi getSolderPack(String solderRoot, String modpackSlug, String mirrorUrl)
      throws RestfulAPIException {
    return new HttpSolderPackApi(solderRoot, modpackSlug, clientIdProvider, mirrorUrl);
  }

  @Override
  public Collection<SolderPackInfo> getPublicSolderPacks(String solderRoot)
      throws RestfulAPIException {
    return internalGetPublicSolderPacks(solderRoot, this);
  }

  public String getMirrorUrl(String solderRoot) throws RestfulAPIException {
    synchronized (mirrorUrls) {
      if (!mirrorUrls.containsKey(solderRoot)) {
        String allPacksUrl = solderRoot + "modpack";
        Solder solder = RestObject.getRestObject(Solder.class, allPacksUrl);
        mirrorUrls.put(solderRoot, solder.getMirrorUrl());
      }

      return mirrorUrls.get(solderRoot);
    }
  }

  @Override
  public Collection<SolderPackInfo> internalGetPublicSolderPacks(
      String solderRoot, ISolderApi packFactory) throws RestfulAPIException {
    LinkedList<SolderPackInfo> allPackApis = new LinkedList<>();
    String allPacksUrl = buildPublicPacksUrl(solderRoot);

    FullModpacks technic = RestObject.getRestObject(FullModpacks.class, allPacksUrl);
    if (technic.getModpacks() == null) {
      throw new RestfulAPIException("Missing Solder public pack catalog");
    }
    for (Map.Entry<String, SolderPackInfo> entry : technic.getModpacks().entrySet()) {
      SolderPackInfo info = entry.getValue();
      if (info == null) {
        throw new RestfulAPIException("Missing Solder pack metadata for " + entry.getKey());
      }
      info.validate(entry.getKey());
      ISolderPackApi solder =
          packFactory.getSolderPack(solderRoot, info.getName(), technic.getMirrorUrl());
      info.setSolder(solder);
      allPackApis.add(info);
    }

    return allPackApis;
  }

  static String buildPublicPacksUrl(String solderRoot) {
    // The public pack list deliberately never sends the client ID: it is a discovery request
    // that no per-pack opt-in can govern
    return solderRoot + "modpack?include=full";
  }
}
