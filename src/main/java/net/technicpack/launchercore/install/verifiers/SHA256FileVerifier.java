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

import net.technicpack.utilslib.CryptoUtils;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.nio.file.Path;

public class SHA256FileVerifier implements IFileVerifier {
    private String expectedHash;

    public SHA256FileVerifier(String expectedHash) {
        this.expectedHash = expectedHash;
    }

    public boolean isFileValid(File file) {
        if (expectedHash == null || expectedHash.isEmpty())
            return false;

        String resultSha256 = CryptoUtils.getSHA256(file);

        boolean hashMatches = expectedHash.equalsIgnoreCase(resultSha256);

        if (!hashMatches)
            Utils.getLogger().warning("SHA256 verification for " + file + " failed. Expected " + expectedHash + ", got " + resultSha256);

        return hashMatches;
    }

    @Override
    public boolean isFileValid(Path path) {
        if (expectedHash == null || expectedHash.isEmpty())
            return false;

        String resultSha256 = CryptoUtils.getSHA256(path);

        boolean hashMatches = expectedHash.equalsIgnoreCase(resultSha256);

        if (!hashMatches)
            Utils.getLogger().warning("SHA256 verification for " + path + " failed. Expected " + expectedHash + ", got " + resultSha256);

        return hashMatches;

    }
}
