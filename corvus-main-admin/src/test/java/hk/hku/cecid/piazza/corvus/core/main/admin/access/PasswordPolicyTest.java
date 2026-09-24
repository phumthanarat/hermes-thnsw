package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import junit.framework.TestCase;

public class PasswordPolicyTest extends TestCase {

    private static String check(String password, String... history) {
        return PasswordPolicy.check(password, "alice", 10, 3, history);
    }

    public void testAcceptsAGoodPassword() {
        assertNull(check("Blue-Harbour-42"));
    }

    public void testLengthAndCharacterKinds() {
        assertNotNull(check("Sh0rt!"));
        assertNotNull("two kinds only", check("alllowercase123"));
        assertNull(check("lowercase-and-digits-9"));
        assertEquals(4, PasswordPolicy.classes("aA1!"));
    }

    public void testUsernameAndWellKnown() {
        assertNotNull(check("Alice-Secret-99"));
        assertNotNull(check("Password12!"));
        assertNotNull(check("Admin123!!"));
        assertNotNull(check("P@ssw0rd-2024"));
        assertNull("a word with more letters is fine", check("Admin-Harbour-42"));
    }

    public void testHistory() {
        String previous = PasswordDigest.mutate("Blue-Harbour-42");
        assertNotNull(check("Blue-Harbour-42", previous));
        assertNull(check("Green-Harbour-43", previous));
    }
}
