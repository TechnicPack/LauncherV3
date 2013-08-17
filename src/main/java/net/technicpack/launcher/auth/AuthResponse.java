package net.technicpack.launcher.auth;

public class AuthResponse {
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
}
