package net.technicpack.launchercore.exception;

import java.io.IOException;

public class RestfulAPIException extends IOException {
	private static final long serialVersionUID = 1L;

	private final Throwable cause;
	private final String message;

	public RestfulAPIException(String message, Throwable cause) {
		this.cause = cause;
		this.message = message;
	}

	public RestfulAPIException(Throwable cause) {
		this(null, cause);
	}

	public RestfulAPIException(String message) {
		this(message, null);
	}

	public RestfulAPIException() {
		this(null, null);
	}

	@Override
	public Throwable getCause() {
		return this.cause;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
