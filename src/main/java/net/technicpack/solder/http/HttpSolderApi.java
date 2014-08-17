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

package net.technicpack.solder.http;

import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.io.FullModpacks;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.io.SolderPackInfo;

import java.util.Collection;
import java.util.LinkedList;

public class HttpSolderApi implements ISolderApi {
    private String clientId;
    private UserModel userModel;

    public HttpSolderApi(String clientId, UserModel userModel) {
        this.clientId = clientId;
        this.userModel = userModel;
    }

    @Override
    public ISolderPackApi getSolderPack(String solderRoot, String modpackSlug) throws RestfulAPIException {
        return new HttpSolderPackApi(solderRoot, modpackSlug, clientId, userModel.getCurrentUser().getDisplayName());
    }

    @Override
    public Collection<SolderPackInfo> getPublicSolderPacks(String solderRoot) throws RestfulAPIException {
        LinkedList<SolderPackInfo> allPackApis = new LinkedList<SolderPackInfo>();
        String allPacksUrl = solderRoot + "modpack/?include=full&cid=" + clientId + "&u=" + userModel.getCurrentUser().getDisplayName();

        FullModpacks technic = RestObject.getRestObject(FullModpacks.class, allPacksUrl);
        for (SolderPackInfo info : technic.getModpacks().values()) {
            ISolderPackApi solder = new HttpSolderPackApi(solderRoot, info.getName(), clientId, userModel.getCurrentUser().getDisplayName());
            info.setSolder(solder);
            allPackApis.add(info);
        }

        return allPackApis;
    }
}
