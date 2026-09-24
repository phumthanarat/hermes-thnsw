package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Time-based one-time passwords (RFC 6238: HMAC-SHA1, 30 s steps, 6
 * digits), as authenticator apps (Google/Microsoft Authenticator, ...)
 * produce them.
 */
public final class Totp {

    static final int STEP_SECONDS = 30;
    static final int DIGITS = 6;
    /** Steps either side of now accepted, for clock drift. */
    static final int WINDOW = 1;

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {
    }

    /** @return a new 160-bit secret, base32 encoded. */
    public static String newSecret() {
        byte[] secret = new byte[20];
        RANDOM.nextBytes(secret);
        return base32(secret);
    }

    /** @return the URI an authenticator app scans (as a QR code). */
    public static String uri(String issuer, String account, String secret) {
        try {
            String label = URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20");
            return "otpauth://totp/" + label + "?secret=" + secret + "&issuer="
                    + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20")
                    + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param lastStep the last step accepted for this user, or null
     * @return the step the code matches, or -1 if it is wrong, too old, or
     *         already used
     */
    public static long verify(String secret, String code, Long lastStep, long nowMillis) {
        if (secret == null || code == null) {
            return -1;
        }
        code = code.replaceAll("\\s", "");
        if (!code.matches("\\d{" + DIGITS + "}")) {
            return -1;
        }
        long now = nowMillis / 1000 / STEP_SECONDS;
        byte[] key = unbase32(secret);
        for (long step = now - WINDOW; step <= now + WINDOW; step++) {
            if (lastStep != null && step <= lastStep.longValue()) {
                continue;
            }
            if (code(key, step).equals(code)) {
                return step;
            }
        }
        return -1;
    }

    static String code(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            String code = Integer.toString(binary % 1000000);
            while (code.length() < DIGITS) {
                code = "0" + code;
            }
            return code;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String base32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] unbase32(String text) {
        text = text.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(text.length() * 5 / 8);
        int buffer = 0, bits = 0;
        for (char c : text.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("not base32: " + c);
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        return out.array();
    }
}
