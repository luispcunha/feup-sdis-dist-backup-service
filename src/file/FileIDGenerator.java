package file;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileIDGenerator {
    public static String generateID(File file) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String data = file.getAbsolutePath() + file.lastModified() + file.length();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] id = digest.digest(data.getBytes("UTF-8"));

        return bytesToHex(id);
    }

    public static String bytesToHex(byte[] hex) {

        StringBuilder ret = new StringBuilder();
        for (byte b : hex) {
            ret.append(String.format("%02x", b));
        }
        return ret.toString();
    }
}