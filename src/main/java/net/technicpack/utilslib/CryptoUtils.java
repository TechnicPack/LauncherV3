package net.technicpack.utilslib;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

public class CryptoUtils {
    public static String getSHA256(File file) {
        try {
            return new DigestUtils(SHA_256).digestAsHex(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkSHA256(File file, String sha256) {
        return checkSHA256(sha256, getSHA256(file));
    }

    public static boolean checkSHA256(String sha256, String otherSha256) {
        return sha256.equalsIgnoreCase(otherSha256);
    }

    public static String getSHA1(File file) {
        try {
            return new DigestUtils(SHA_1).digestAsHex(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkSHA1(File file, String sha1) {
        return checkSHA1(sha1, getSHA1(file));
    }

    public static boolean checkSHA1(String sha1, String otherSha1) {
        return sha1.equalsIgnoreCase(otherSha1);
    }

    public static String getMD5(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String md5 = DigestUtils.md5Hex(fis);
            fis.close();
            return md5;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkMD5(File file, String md5) {
        return checkMD5(md5, getMD5(file));
    }

    public static boolean checkMD5(String md5, String otherMd5) {
        return md5.equalsIgnoreCase(otherMd5);
    }
}
