package net.technicpack.launchercore.exception;

/**
 *
 */
public class ResponseException extends AuthenticationException {
    private static final long serialVersionUID = 5887385045789344444L;
    private String error;

    public ResponseException(String error, String errorMessage) {
        super(errorMessage);
        this.error = error;
    }

    public ResponseException(String error, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
