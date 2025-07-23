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

package net.technicpack.launcher.settings.migration;

import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.io.UserStore;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.NewsArticle;
import net.technicpack.rest.RestfulAPIException;

import java.util.LinkedList;
import java.util.List;

public class InitialV3Migrator implements IMigrator {
    private IPlatformApi platformApi;

    public InitialV3Migrator(IPlatformApi platformApi) {
        this.platformApi = platformApi;
    }

    @Override
    public String getMigrationVersion() {
        return "0";
    }

    @Override
    public String getMigratedVersion() {
        return "1";
    }

    @Override
    public void migrate(TechnicSettings settings, InstalledPackStore packStore, LauncherFileSystem fileSystem, UserStore users) {
        //A fresh install/upgrade from v2 shouldn't show the latest news as being new
        int maxNewsId = 0;

        try {
            for (NewsArticle article : platformApi.getNews().getArticles()) {
                int newsId = article.getId();

                if (newsId > maxNewsId)
                    maxNewsId = newsId;
            }

            settings.setLatestNewsArticle(maxNewsId);
        } catch (RestfulAPIException ex) {
            //Just kill the exception & go with ID 0
        }

        List<ModpackModel> deletePacks = new LinkedList<>();
        for (String packName : packStore.getPackNames()) {
            InstalledPack pack = packStore.getInstalledPacks().get(packName);
            ModpackModel model = new ModpackModel(pack, null, packStore, fileSystem);

            if (!model.getInstalledDirectory().exists()) {
                deletePacks.add(model);
            }
        }

        for (ModpackModel deletePack : deletePacks) {
            deletePack.delete();
        }

        packStore.save();
    }
}
