package net.technicpack.launchercore.auth;

public class RefreshRequest extends Response {
	private String accessToken;
	private String clientToken;
	private Profile selectedProfile;

	public RefreshRequest() {

	}
}
