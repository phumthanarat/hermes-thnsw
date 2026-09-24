package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import junit.framework.TestCase;

public class PasswordDigestTest extends TestCase {

    /** Made in the app container with Tomcat 8.0.32's own tool:
     *  digest.sh -a SHA-256 -i 1000 -s 16 -h ...MessageDigestCredentialHandler admin */
    private static final String TOMCAT_ADMIN =
            "e78ddbc266fd904b226d6282193aec29$1000$1c07ef129a6f56b20b9ea84aedecb6f7bb685ba1c27a2a8bc4c0542a3fe183c1";

    public void testMatchesWhatTomcatMakes() {
        assertTrue(PasswordDigest.matches("admin", TOMCAT_ADMIN));
        assertFalse(PasswordDigest.matches("Admin", TOMCAT_ADMIN));
        assertFalse(PasswordDigest.matches("admin ", TOMCAT_ADMIN));
    }

    public void testMutateRoundTripsWithFreshSalt() {
        String a = PasswordDigest.mutate("s3cret-pass");
        String b = PasswordDigest.mutate("s3cret-pass");
        assertFalse("each password gets its own salt", a.equals(b));
        assertTrue(a.matches("[0-9a-f]{32}\\$10000\\$[0-9a-f]{64}"));
        assertTrue(PasswordDigest.matches("s3cret-pass", a));
        assertFalse(PasswordDigest.matches("s3cret-pasS", a));
    }

    public void testRejectsMalformedStoredValues() {
        assertFalse(PasswordDigest.matches("admin", null));
        assertFalse(PasswordDigest.matches("admin", "admin"));
        assertFalse(PasswordDigest.matches("admin", "zz$1$00"));
        assertFalse(PasswordDigest.matches(null, TOMCAT_ADMIN));
    }
}
