package thaw.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by nakaze on 27/12/16.
 */
public class Utils {
    public final static boolean checkPassword(String shaDigest, String password) {
        return shaDigest.equals(toSHA256(password));
    }

    public final static String toSHA256(String passwd) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        md.update(new byte[]{5, 3, 3, 'd'});
        byte[] data = md.digest(passwd.getBytes(StandardCharsets.UTF_8));
        return IntStream.range(0, data.length)
                .mapToObj(i -> String.format("%02x", data[i]))
                .collect(Collectors.joining());
    }
}
