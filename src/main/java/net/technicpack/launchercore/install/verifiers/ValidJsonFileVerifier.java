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
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class ValidJsonFileVerifier implements IFileVerifier {
    private Gson validatingGson;

    public ValidJsonFileVerifier(Gson validatingGson) {
        this.validatingGson = validatingGson;
    }

    @Override
    public boolean isFileValid(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject obj = validatingGson.fromJson(reader, JsonObject.class);

            return (obj != null);
        } catch (JsonSyntaxException e) {
            Utils.getLogger().log(Level.WARNING, String.format("JSON validation failed for %s", file.getAbsolutePath()), e);
            return false;
        } catch (JsonIOException | IOException ex) {
            Utils.getLogger().log(Level.SEVERE, String.format("An I/O error happened while validating %s", file.getAbsolutePath()), ex);
        }

        return false;
    }

    @Override
    public boolean isFileValid(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = validatingGson.fromJson(reader, JsonObject.class);

            return (obj != null);
        } catch (JsonSyntaxException e) {
            Utils.getLogger().log(Level.WARNING, String.format("JSON validation failed for %s", path), e);
            return false;
        } catch (JsonIOException | IOException ex) {
            Utils.getLogger().log(Level.SEVERE, String.format("An I/O error happened while validating %s", path), ex);
        }

        return false;
    }
}
