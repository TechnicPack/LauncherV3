/*
 * This file is part of Technic Launcher.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.technicpack.launchercore.mirror.MirrorStore;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

public class Utils {
    private static final Gson gson;
    private static final Gson mojangGson;
    private static final Logger logger = Logger.getLogger("net.technicpack.launcher.Main");

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.enableComplexMapKeySerialization();
        mojangGson = builder.create();
        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    public static Gson getGson() {
        return gson;
    }

    public static Gson getMojangGson() {
        return mojangGson;
    }

    public static File getLauncherDirectory() {
        return Directories.instance.getLauncherDirectory();
    }

    public static File getSettingsDirectory() {
        return Directories.instance.getSettingsDirectory();
    }

    public static File getCacheDirectory() {
        return Directories.instance.getCacheDirectory();
    }

    public static File getAssetsDirectory() {
        return Directories.instance.getAssetsDirectory();
    }

    public static File getModpacksDirectory() {
        return Directories.instance.getModpacksDirectory();
    }

    public static Logger getLogger() {
        return logger;
    }

    /**
     * Establishes an HttpURLConnection from a URL, with the correct configuration to receive content from the given URL.
     *
     * @param url The URL to set up and receive content from
     * @return A valid HttpURLConnection
     * @throws IOException The openConnection() method throws an IOException and the calling method is responsible for handling it.
     */
    public static HttpURLConnection openHttpConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(false);
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        HttpURLConnection.setFollowRedirects(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    /**
     * Opens an HTTP connection to a web URL and tests that the response is a valid 200-level code
     * and we can successfully open a stream to the content.
     *
     * @param urlLoc The HTTP URL indicating the location of the content.
     * @return True if the content can be accessed successfully, false otherwise.
     */
    public static boolean pingHttpURL(String urlLoc, MirrorStore mirrorStore) {
        InputStream stream = null;
        try {
            final URL url = mirrorStore.getFullUrl(urlLoc);
            final HttpURLConnection conn = openHttpConnection(url);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            int responseFamily = responseCode / 100;

            if (responseFamily == 2) {
                stream = conn.getInputStream();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static boolean sendTracking(String category, String action, String label) {
        String url = "http://www.google-analytics.com/collect";
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");

            String urlParameters = "v=1&tid=UA-30896795-3&cid=" + Settings.getClientId() + "&t=event&ec=" + category + "&ea=" + action + "&el=" + label;

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("Analytics Response [" + category + "]: " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();


            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
