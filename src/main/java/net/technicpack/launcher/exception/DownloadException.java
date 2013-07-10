package net.technicpack.launcher.exception;

import java.io.IOException;

public class DownloadException extends IOException {
	private static final long serialVersionUID = 1L;

	private final Throwable cause;
	private final String message;

	public DownloadException(String message, Throwable cause) {
		this.cause = cause;
		this.message = message;
	}

	public DownloadException(Throwable cause) {
		this(null, cause);
	}

	public DownloadException(String message) {
		this(message, null);
	}

	public DownloadException() {
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
