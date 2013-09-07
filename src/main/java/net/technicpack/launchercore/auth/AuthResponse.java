package net.technicpack.launchercore.auth;

import java.util.Arrays;

public class AuthResponse extends Response {
	private String accessToken;
	private String clientToken;
	private Profile[] availableProfiles;
	private Profile selectedProfile;

	public String getAccessToken() {
		return accessToken;
	}

	public String getClientToken() {
		return clientToken;
	}

	public Profile[] getAvailableProfiles() {
		return availableProfiles;
	}

	public Profile getSelectedProfile() {
		return selectedProfile;
	}

	@Override
	public String toString() {
		return "AuthResponse{" +
				"accessToken='" + accessToken + '\'' +
				", clientToken='" + clientToken + '\'' +
				", availableProfiles=" + Arrays.toString(availableProfiles) +
				", selectedProfile=" + selectedProfile +
				'}';
	}
}
