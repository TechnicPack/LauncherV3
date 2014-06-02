package net.technicpack.launchercore.exception;

import java.io.IOException;

public class BuildInaccessibleException extends IOException {
    private String packDisplayName;
    private String build;
    private Throwable cause;
    private static final long serialVersionUID = -4905270588640056830L;

    public BuildInaccessibleException(String displayName, String build) {
        this.packDisplayName = displayName;
        this.build = build;
    }

    public BuildInaccessibleException(String displayName, String build, Throwable cause) {
        this(displayName, build);
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        if (this.cause != null) {
            Throwable rootCause = this.cause;

            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            return "An error was raised while attempting to read pack info for modpack " + packDisplayName + ", build " + build + ": " + rootCause.getMessage();
        } else {
            return "The pack host returned unrecognizable garbage while attempting to read pack info for modpack " + packDisplayName + ", build " + build + ".";
        }
    }

    @Override
    public synchronized Throwable getCause() {
        return cause;
    }
}
