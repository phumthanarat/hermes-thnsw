package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

public class TotpTest extends TestCase {

    /** RFC 6238 appendix B (SHA-1 seed), truncated to 6 digits. */
    private static final byte[] RFC_KEY = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    public void testRfc6238Vectors() {
        assertEquals("287082", Totp.code(RFC_KEY, 59L / 30));
        assertEquals("081804", Totp.code(RFC_KEY, 1111111109L / 30));
        assertEquals("050471", Totp.code(RFC_KEY, 1111111111L / 30));
        assertEquals("005924", Totp.code(RFC_KEY, 1234567890L / 30));
        assertEquals("279037", Totp.code(RFC_KEY, 2000000000L / 30));
    }

    public void testBase32RoundTrip() {
        String secret = Totp.base32(RFC_KEY);
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", secret);
        assertEquals(new String(RFC_KEY, StandardCharsets.US_ASCII),
                new String(Totp.unbase32(secret), StandardCharsets.US_ASCII));
        assertEquals(32, Totp.newSecret().length());
    }

    public void testVerifyWindowAndReplay() {
        String secret = Totp.base32(RFC_KEY);
        long now = 1111111111L * 1000;
        long step = now / 1000 / 30;
        String current = Totp.code(RFC_KEY, step);
        assertEquals(step, Totp.verify(secret, current, null, now));
        assertEquals(step, Totp.verify(secret, current.substring(0, 3) + " " + current.substring(3), null, now));
        // a code already used is refused
        assertEquals(-1, Totp.verify(secret, current, Long.valueOf(step), now));
        // one step either side is accepted, two is not
        assertEquals(step - 1, Totp.verify(secret, Totp.code(RFC_KEY, step - 1), null, now));
        assertEquals(-1, Totp.verify(secret, Totp.code(RFC_KEY, step - 2), null, now));
        assertEquals(-1, Totp.verify(secret, "abcdef", null, now));
        assertEquals(-1, Totp.verify(null, current, null, now));
    }

    public void testUri() {
        String uri = Totp.uri("Hermes2+ (gw)", "admin", "ABC");
        assertTrue(uri, uri.startsWith("otpauth://totp/Hermes2%2B%20%28gw%29%3Aadmin?secret=ABC&issuer="));
    }
}
