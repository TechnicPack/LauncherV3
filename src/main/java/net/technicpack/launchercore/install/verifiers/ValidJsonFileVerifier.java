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

package net.technicpack.launchercore.install.verifiers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class ValidJsonFileVerifier implements IFileVerifier {
    private Gson validatingGson;

    public ValidJsonFileVerifier(Gson validatingGson) {
        this.validatingGson = validatingGson;
    }

    @Override
    public boolean isFileValid(File file) {
        try {
            String json = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            JsonObject obj = validatingGson.fromJson(json, JsonObject.class);

            return (obj != null);
        } catch (Exception ex) {
            Utils.getLogger().log(Level.SEVERE, "An exception was raised while verifying " + file.getAbsolutePath() + "- this probably just means the file is invalid, in which case this is not an error:", ex);
        }

        return false;
    }
}
