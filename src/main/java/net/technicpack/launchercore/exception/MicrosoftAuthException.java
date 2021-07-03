package net.technicpack.launchercore.exception;

/**
 *
 */
public class MicrosoftAuthException extends ResponseException {
    private final ExceptionType type;

    public MicrosoftAuthException(ExceptionType type, String message) {
        super("Auth Error", message);
        this.type = type;
    }

    public MicrosoftAuthException(ExceptionType type, String message, Throwable cause) {
        super("Auth Error", message, cause);
        this.type = type;
    }

    public ExceptionType getType() {
        return type;
    }

    public enum ExceptionType {
        REQUEST,
        OAUTH,
        XBOX,
        XSTS,
        XBOX_MINECRAFT,
        MINECRAFT_PROFILE,
        UNDERAGE,
        NO_XBOX_ACCOUNT,
        NO_MINECRAFT
    }
}
