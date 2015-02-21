/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.auth;

import com.google.common.base.Charsets;
import net.technicpack.launchercore.auth.IAuthResponse;
import net.technicpack.launchercore.auth.IGameAuthService;
import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.auth.io.Agent;
import net.technicpack.minecraftcore.mojang.auth.request.AuthRequest;
import net.technicpack.minecraftcore.mojang.auth.request.RefreshRequest;
import net.technicpack.minecraftcore.mojang.auth.response.AuthResponse;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthenticationService implements IGameAuthService<MojangUser> {
	private static final String AUTH_SERVER = "https://authserver.mojang.com/";

	public AuthResponse requestRefresh(MojangUser mojangUser) throws AuthenticationNetworkFailureException {
		RefreshRequest refreshRequest = new RefreshRequest(mojangUser.getAccessToken(), mojangUser.getClientToken());
		String data = MojangUtils.getGson().toJson(refreshRequest);

		AuthResponse response;
		try {
			String returned = postJson(AUTH_SERVER + "refresh", data);
			response = MojangUtils.getGson().fromJson(returned, AuthResponse.class);
		} catch (IOException e) {
			throw new AuthenticationNetworkFailureException("authserver.mojang.com", e);
		}

		return response;
	}

	private String postJson(String url, String data) throws IOException {
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
        String returnable = null;
		try {
			stream = connection.getInputStream();
            returnable = IOUtils.toString(stream, Charsets.UTF_8);
		} catch (IOException e) {
			stream = connection.getErrorStream();

			if (stream == null) {
				throw e;
			}
		} finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {}
        }

        return returnable;
	}

	public AuthResponse requestLogin(String username, String password, String clientToken) throws AuthenticationNetworkFailureException {
		Agent agent = new Agent("Minecraft", "1");

		AuthRequest request = new AuthRequest(agent, username, password, clientToken);
		String data = MojangUtils.getGson().toJson(request);

		AuthResponse response;
		try {
			String returned = postJson(AUTH_SERVER + "authenticate", data);
			response = MojangUtils.getGson().fromJson(returned, AuthResponse.class);
		} catch (IOException e) {
			throw new AuthenticationNetworkFailureException("authserver.mojang.com", e);
		}
		return response;
	}

    public MojangUser createClearedUser(String username, IAuthResponse response) {
        return new MojangUser(username, (AuthResponse)response);
    }

    public MojangUser createOfflineUser(String displayName) {
        return new MojangUser(displayName);
    }
}
