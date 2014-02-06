package net.technicpack.launchercore.util.verifiers;

import net.technicpack.launchercore.util.MD5Utils;

import java.io.File;

public class MD5FileVerifier implements IFileVerifier {
    private String md5Hash;

    public MD5FileVerifier(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public boolean isFileValid(File file) {
        if (md5Hash == null || md5Hash.isEmpty())
            return false;

        String resultMD5 = MD5Utils.getMD5(file);

        System.out.println("Expected MD5: " + md5Hash + " Calculated MD5: " + resultMD5);
        return (md5Hash.equalsIgnoreCase(resultMD5));
    }
}
