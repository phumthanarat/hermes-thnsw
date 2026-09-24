package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO.UserSecurity;

/**
 * What the console keeps per user besides Tomcat's user database: when the
 * password was last changed (for expiry), the previous passwords (not to
 * be reused), and the two-factor (TOTP) secret.
 */
public final class Accounts {

    private static final long DAY = 24L * 3600 * 1000;

    private Accounts() {
    }

    static UserSecurity load(String username) throws Exception {
        UserSecurity s = SecuritySettings.dao().getUserSecurity(username);
        if (s == null) {
            s = new UserSecurity();
            s.username = username;
        }
        return s;
    }

    static void save(UserSecurity s) throws Exception {
        SecuritySettings.dao().saveUserSecurity(s);
    }

    /** @return the user's previous password digests, newest first. */
    public static String[] history(String username) {
        try {
            UserSecurity s = load(username);
            return s.passwordHistory == null || s.passwordHistory.trim().isEmpty() ? new String[0]
                    : s.passwordHistory.trim().split("\\s+");
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Records a new password: its digest joins the history (the current
     * password counts, so it can't be set again) and its age restarts.
     */
    public static void passwordChanged(String username, String digest) {
        try {
            UserSecurity s = load(username);
            List<String> history = new ArrayList<String>();
            history.add(digest);
            history.addAll(Arrays.asList(history(username)));
            int keep = Math.max(1, SecuritySettings.getInt(SecuritySettings.PASSWORD_HISTORY));
            StringBuilder joined = new StringBuilder();
            for (String h : history.subList(0, Math.min(keep, history.size()))) {
                if (joined.length() + h.length() + 1 > 4000) {
                    break;
                }
                joined.append(joined.length() == 0 ? "" : " ").append(h);
            }
            s.passwordHistory = joined.toString();
            s.passwordChanged = new Timestamp(System.currentTimeMillis());
            save(s);
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to record the password change of " + username, e);
        }
    }

    /** @return whether the password is older than the maximum age. */
    public static boolean passwordExpired(String username) {
        int days = SecuritySettings.getInt(SecuritySettings.PASSWORD_MAX_AGE_DAYS);
        if (days <= 0) {
            return false;
        }
        try {
            UserSecurity s = load(username);
            if (s.passwordChanged == null) {
                // not known yet (e.g. the first admin): start counting now
                s.passwordChanged = new Timestamp(System.currentTimeMillis());
                save(s);
                return false;
            }
            return System.currentTimeMillis() - s.passwordChanged.getTime() > days * DAY;
        } catch (Exception e) {
            return false;
        }
    }

    public static Timestamp passwordChangedAt(String username) {
        try {
            return load(username).passwordChanged;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean twoFactorEnabled(String username) {
        try {
            UserSecurity s = load(username);
            return s.totpEnabled && s.totpSecret != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Administrators must use two-factor sign-in when the setting says so. */
    public static boolean twoFactorRequired(AccessLevel level) {
        return level == AccessLevel.ADMINISTRATOR
                && SecuritySettings.getBoolean(SecuritySettings.TWO_FACTOR_REQUIRED_FOR_ADMINS);
    }

    /** @return a new secret, saved but not yet active until confirmed. */
    public static String startTwoFactor(String username) throws Exception {
        UserSecurity s = load(username);
        if (s.totpEnabled && s.totpSecret != null) {
            throw new IllegalStateException("two-factor sign-in is already set up");
        }
        if (s.totpSecret == null) {
            s.totpSecret = Totp.newSecret();
            save(s);
        }
        return s.totpSecret;
    }

    /** @return the pending secret of a user setting up two-factor sign-in. */
    public static String pendingSecret(String username) {
        try {
            UserSecurity s = load(username);
            return s.totpEnabled ? null : s.totpSecret;
        } catch (Exception e) {
            return null;
        }
    }

    /** Activates the pending secret once the user shows a code from it. */
    public static boolean confirmTwoFactor(String username, String code) throws Exception {
        UserSecurity s = load(username);
        long step = Totp.verify(s.totpSecret, code, s.totpLastStep, System.currentTimeMillis());
        if (step < 0) {
            return false;
        }
        s.totpEnabled = true;
        s.totpLastStep = Long.valueOf(step);
        save(s);
        return true;
    }

    /** Checks a sign-in code; each code is accepted once. */
    public static boolean verifyTwoFactor(String username, String code) throws Exception {
        UserSecurity s = load(username);
        if (!s.totpEnabled) {
            return false;
        }
        long step = Totp.verify(s.totpSecret, code, s.totpLastStep, System.currentTimeMillis());
        if (step < 0) {
            return false;
        }
        s.totpLastStep = Long.valueOf(step);
        save(s);
        return true;
    }

    /** Removes the user's two-factor setup (they set it up again). */
    public static void resetTwoFactor(String username) throws Exception {
        UserSecurity s = load(username);
        s.totpSecret = null;
        s.totpEnabled = false;
        s.totpLastStep = null;
        save(s);
    }

    public static void forget(String username) {
        try {
            SecuritySettings.dao().removeUserSecurity(username);
        } catch (Exception e) {
            AdminMainProcessor.core.log.warn("Unable to remove the security record of " + username);
        }
    }

    static ConsoleSecurityDAO dao() throws Exception {
        return SecuritySettings.dao();
    }
}
