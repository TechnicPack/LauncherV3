package net.technicpack.launchercore.auth;

public class Response {
	private String error;
	private String errorMessage;
	private String cause;

	public String getError() {
		return error;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getCause() {
		return cause;
	}

	@Override
	public String toString() {
		return "Response{" +
				"error='" + error + '\'' +
				", errorMessage='" + errorMessage + '\'' +
				", cause='" + cause + '\'' +
				'}';
	}
}
