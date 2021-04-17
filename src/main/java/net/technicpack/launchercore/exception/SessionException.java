package net.technicpack.launchercore.exception;

public class SessionException extends AuthenticationException {
    private static final long serialVersionUID = 5883333045789342851L;

    public SessionException(String message) {
        super(message);
    }

    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
