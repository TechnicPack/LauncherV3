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

package net.technicpack.launchercore.image.face;

import net.technicpack.launchercore.image.IImageStore;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class WebAvatarImageStore implements IImageStore<AuthorshipInfo> {
    private MirrorStore mirrorStore;

    public WebAvatarImageStore(MirrorStore mirrorStore) {
        this.mirrorStore = mirrorStore;
    }

    @Override
    public boolean canDownloadImage(AuthorshipInfo key, File target) {
        return true;
    }

    @Override
    public void downloadImage(AuthorshipInfo key, File target) {
        try {
            mirrorStore.downloadFile(key.getAvatar(), key.getUser(), target.getAbsolutePath());
        } catch (InterruptedException ex) {
            //User cancel
        } catch (IOException e) {
            Utils.getLogger().log(Level.INFO, "Error downloading user avatar: " + key.getUser(), e);
        }
    }

    @Override
    public String getJobKey(AuthorshipInfo key) {
        return "user-avatar-" + key.getUser();
    }

    @Override
    public boolean canRetry(AuthorshipInfo key) {
        return false;
    }
}
