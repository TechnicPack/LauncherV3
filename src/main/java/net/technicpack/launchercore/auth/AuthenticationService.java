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

package net.technicpack.launchercore.auth;

import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;
import net.technicpack.launchercore.install.User;
import net.technicpack.launchercore.install.Users;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthenticationService {
	private static final String AUTH_SERVER = "https://authserver.mojang.com/";

	public static AuthResponse requestRefresh(User user) throws AuthenticationNetworkFailureException {
		RefreshRequest refreshRequest = new RefreshRequest(user.getAccessToken(), user.getClientToken());
		String data = Utils.getMojangGson().toJson(refreshRequest);

		AuthResponse response;
		try {
			String returned = postJson(AUTH_SERVER + "refresh", data);
			System.out.println(returned);
			response = Utils.getMojangGson().fromJson(returned, AuthResponse.class);
		} catch (IOException e) {
			throw new AuthenticationNetworkFailureException(e);
		}

		return response;
	}

	private static String postJson(String url, String data) throws IOException {
		byte[] rawData = data.getBytes("UTF-8");
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		connection.setRequestProperty("Content-Length", rawData.length + "");
		connection.setRequestProperty("Content-Language", "en-US");

		DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(rawData);
		writer.flush();
		writer.close();

		InputStream stream = null;
		try {
			stream = connection.getInputStream();
		} catch (IOException e) {
			stream = connection.getErrorStream();

			if (stream == null) {
				throw e;
			}
		}

		return IOUtils.toString(stream);
	}

	public static AuthResponse requestLogin(String username, String password, String clientToken) throws AuthenticationNetworkFailureException {
		Agent agent = new Agent("Minecraft", "1");

		AuthRequest request = new AuthRequest(agent, username, password, clientToken);
		String data = Utils.getMojangGson().toJson(request);

		AuthResponse response;
		try {
			String returned = postJson(AUTH_SERVER + "authenticate", data);
			System.out.println("Auth: " + returned);
			response = Utils.getMojangGson().fromJson(returned, AuthResponse.class);
		} catch (IOException e) {
			throw new AuthenticationNetworkFailureException(e);
		}
		return response;
	}
}
