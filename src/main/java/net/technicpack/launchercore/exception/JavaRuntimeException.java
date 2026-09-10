package net.technicpack.launchercore.exception;

import java.io.IOException;

/** A Java executable failed to provide the metadata required to install or launch a pack. */
public class JavaRuntimeException extends IOException {
  public JavaRuntimeException(String message) {
    super(message);
  }

  public JavaRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
