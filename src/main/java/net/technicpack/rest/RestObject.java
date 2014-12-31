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

package net.technicpack.rest;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class RestObject {
    private static final Gson gson = new Gson();

    private String error;

    public boolean hasError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    public static <T extends RestObject> T postRestObject(Class<T> restObject, String url) throws RestfulAPIException {
        InputStream stream = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            stream = conn.getInputStream();
            String data = IOUtils.toString(stream);
            T result = gson.fromJson(data, restObject);

            if (result == null) {
                throw new RestfulAPIException("Unable to access URL [" + url + "]");
            }

            if (result.hasError()) {
                throw new RestfulAPIException("Error in response: " + result.getError());
            }

            return result;
        } catch (SocketTimeoutException e) {
            throw new RestfulAPIException("Timed out accessing URL [" + url + "]", e);
        } catch (MalformedURLException e) {
            throw new RestfulAPIException("Invalid URL [" + url + "]", e);
        } catch (JsonParseException e) {
            throw new RestfulAPIException("Error parsing response JSON at URL [" + url + "]", e);
        } catch (IOException e) {
            throw new RestfulAPIException("Error accessing URL [" + url + "]", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static <T extends RestObject> T getRestObject(Class<T> restObject, String url) throws RestfulAPIException {
        InputStream stream = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            stream = conn.getInputStream();
            String data = IOUtils.toString(stream);
            T result = gson.fromJson(data, restObject);

            if (result == null) {
                throw new RestfulAPIException("Unable to access URL [" + url + "]");
            }

            if (result.hasError()) {
                throw new RestfulAPIException("Error in response: " + result.getError());
            }

            return result;
        } catch (SocketTimeoutException e) {
            throw new RestfulAPIException("Timed out accessing URL [" + url + "]", e);
        } catch (MalformedURLException e) {
            throw new RestfulAPIException("Invalid URL [" + url + "]", e);
        } catch (JsonParseException e) {
            throw new RestfulAPIException("Error parsing response JSON at URL [" + url + "]", e);
        } catch (IOException e) {
            throw new RestfulAPIException("Error accessing URL [" + url + "]", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
