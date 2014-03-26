package net.technicpack.launchercore.install.verifiers;

import java.io.File;

public interface IFileVerifier {
    boolean isFileValid(File file);
}
