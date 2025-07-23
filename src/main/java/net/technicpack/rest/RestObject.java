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
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.TechnicConstants;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public abstract class RestObject {
    private static final Gson gson = new Gson();

    @SuppressWarnings("unused")
    private String error;

    public static <T extends RestObject> T getRestObject(Class<T> restObject, String url) throws RestfulAPIException {
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", TechnicConstants.getUserAgent());
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            T result;

            try (Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                result = gson.fromJson(reader, restObject);
            }

            if (result == null) {
                throw new RestfulAPIException(String.format("Unable to access URL [%s]", url));
            }

            if (result.hasError()) {
                throw new RestfulAPIException(String.format("Error in response: %s", result.getError()));
            }

            return result;
        } catch (SocketTimeoutException e) {
            throw new RestfulAPIException(String.format("Timed out accessing URL [%s]", url), e);
        } catch (MalformedURLException e) {
            throw new RestfulAPIException(String.format("Invalid URL [%s]", url), e);
        } catch (JsonSyntaxException e) {
            throw new RestfulAPIException(String.format("Error parsing response JSON at URL [%s]", url), e);
        } catch (JsonIOException | IOException e) {
            throw new RestfulAPIException(String.format("Error accessing URL [%s]", url), e);
        }
    }

    public boolean hasError() {
        return error != null;
    }

    public String getError() {
        return error;
    }
}
