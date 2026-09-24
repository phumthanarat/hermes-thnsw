package hk.hku.cecid.piazza.corvus.core.main.admin.access;

/**
 * The password rules of Access > Security Settings: a minimum length, a
 * minimum number of character classes (lower case, upper case, digits,
 * others), not the username, not a well-known password, and not one of the
 * user's recent passwords.
 */
public final class PasswordPolicy {

    private static final String[] WELL_KNOWN = { "admin", "password", "hermes", "corvus",
            "changeit", "12345678", "123456789", "1234567890", "qwerty", "letmein", "welcome" };

    private PasswordPolicy() {
    }

    /**
     * @param history the user's previous password digests, newest first
     * @return why the password is refused, or null if it is acceptable
     */
    public static String check(String password, String username, int minLength, int minClasses,
            String[] history) {
        if (password == null || password.length() < minLength) {
            return "a password needs at least " + minLength + " characters";
        }
        if (classes(password) < minClasses) {
            return "a password needs at least " + minClasses
                    + " of: lower case, upper case, digits, other characters";
        }
        String lower = password.toLowerCase();
        if (username != null && lower.contains(username.toLowerCase())) {
            return "a password must not contain the username";
        }
        // a well-known word dressed up with digits/symbols (Admin123!, P@ssword1)
        String core = lower.replaceAll("^[^a-z@$]+|[^a-z]+$", "");
        String letters = core.replace('@', 'a').replace('0', 'o').replace('$', 's')
                .replace('1', 'i').replace('3', 'e').replaceAll("[^a-z]", "");
        for (String known : WELL_KNOWN) {
            if (lower.equals(known) || letters.equals(known.replaceAll("[^a-z]", ""))
                    && !letters.isEmpty()) {
                return "that password is too easy to guess";
            }
        }
        for (String previous : history == null ? new String[0] : history) {
            if (PasswordDigest.matches(password, previous)) {
                return "choose a password you haven't used recently";
            }
        }
        return null;
    }

    /** The rules as currently set. */
    public static String check(String password, String username, String[] history) {
        return check(password, username, SecuritySettings.getInt(SecuritySettings.PASSWORD_MIN_LENGTH),
                SecuritySettings.getInt(SecuritySettings.PASSWORD_MIN_CLASSES), history);
    }

    /** One line describing the rules, for the password forms. */
    public static String describe() {
        return "At least " + SecuritySettings.getInt(SecuritySettings.PASSWORD_MIN_LENGTH)
                + " characters, with " + SecuritySettings.getInt(SecuritySettings.PASSWORD_MIN_CLASSES)
                + " of: lower case, upper case, digits, other characters; not the username;"
                + " not one of your last " + SecuritySettings.getInt(SecuritySettings.PASSWORD_HISTORY)
                + " passwords.";
    }

    static int classes(String password) {
        boolean lower = false, upper = false, digit = false, other = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                other = true;
            }
        }
        return (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (other ? 1 : 0);
    }
}
