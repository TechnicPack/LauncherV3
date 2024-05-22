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

package net.technicpack.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.technicpack.launchercore.TechnicConstants;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RestObject {
    private static final Gson gson = new Gson();

    private String error;

    public boolean hasError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    public static <T extends RestObject> T getRestObject(Class<T> restObject, String url) throws RestfulAPIException {
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", TechnicConstants.getUserAgent());
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (InputStream stream = conn.getInputStream()) {
                String data = IOUtils.toString(stream, StandardCharsets.UTF_8);
                T result = gson.fromJson(data, restObject);

                if (result == null) {
                    throw new RestfulAPIException("Unable to access URL [" + url + "]");
                }

                if (result.hasError()) {
                    throw new RestfulAPIException("Error in response: " + result.getError());
                }

                return result;
            }
        } catch (SocketTimeoutException e) {
            throw new RestfulAPIException("Timed out accessing URL [" + url + "]", e);
        } catch (MalformedURLException e) {
            throw new RestfulAPIException("Invalid URL [" + url + "]", e);
        } catch (JsonParseException e) {
            throw new RestfulAPIException("Error parsing response JSON at URL [" + url + "]", e);
        } catch (IOException e) {
            throw new RestfulAPIException("Error accessing URL [" + url + "]", e);
        }
    }
}
