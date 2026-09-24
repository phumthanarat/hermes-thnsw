package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Makes and checks stored passwords in the format of Tomcat's
 * MessageDigestCredentialHandler (server.xml): "salt$iterations$digest",
 * hex encoded, where digest = SHA-256(salt + password) re-hashed
 * iterations - 1 more times. Tomcat's realm verifies what this writes.
 */
public final class PasswordDigest {

    static final String ALGORITHM = "SHA-256";
    static final int ITERATIONS = 10000;
    static final int SALT_LENGTH = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordDigest() {
    }

    /** @return the stored form of password, with a fresh random salt. */
    public static String mutate(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return toHex(salt) + "$" + ITERATIONS + "$" + digest(salt, ITERATIONS, password);
    }

    /** @return whether password matches a stored "salt$iterations$digest". */
    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        try {
            String expected = digest(fromHex(parts[0]), Integer.parseInt(parts[1]), password);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                    parts[2].toLowerCase().getBytes(StandardCharsets.US_ASCII));
        } catch (RuntimeException e) {
            return false;
        }
    }

    static String digest(byte[] salt, int iterations, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            md.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] result = md.digest();
            for (int i = 1; i < iterations; i++) {
                result = md.digest(result);
            }
            return toHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return out.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
