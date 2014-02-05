package net.technicpack.launchercore.util.verifiers;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class FileSizeVerifier implements IFileVerifier {
    private long size;

    public FileSizeVerifier(long size) {
        this.size = size;
    }

    @Override
    public boolean isFileValid(File file) {
        return FileUtils.sizeOf(file) == size;
    }
}
