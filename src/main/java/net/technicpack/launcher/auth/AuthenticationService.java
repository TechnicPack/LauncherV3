package net.technicpack.launcher.auth;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthenticationService {
	private static final String AUTH_SERVER = "https://authserver.mojang.com/";
	private final Gson gson;

	public AuthenticationService() {
		gson = new Gson();
	}

	public AuthResponse requestLogin(String username, String password, String clientToken) {
		Agent agent = new Agent("Minecraft", "1");

		AuthRequest request = new AuthRequest(agent, username, password, clientToken);
		String data = gson.toJson(request);

		AuthResponse response;
		try {
			String returned = postJson(AUTH_SERVER + "authenticate", data);
			response = gson.fromJson(returned, AuthResponse.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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
}
