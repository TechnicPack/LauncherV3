/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2018 Syndicate, LLC
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

package net.technicpack.launchercore.install.verifiers;

import net.technicpack.utilslib.SHA1Utils;
import net.technicpack.utilslib.Utils;

import java.io.File;

public class SHA1FileVerifier implements IFileVerifier {
    private String sha1Hash;

    public SHA1FileVerifier(String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    public boolean isFileValid(File file) {
        if (sha1Hash == null || sha1Hash.isEmpty())
            return false;

        String resultSha1 = SHA1Utils.getSHA1(file);

        boolean hashMatches = sha1Hash.equalsIgnoreCase(resultSha1);

        if (!hashMatches)
            Utils.getLogger().warning("SHA1 verification for " + file + " failed. Expected " + sha1Hash + ", got " + resultSha1);

        return hashMatches;
    }
}
