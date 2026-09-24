package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.util.LinkedHashMap;
import java.util.Map;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;

/**
 * The admin console's security settings (Access > Security Settings),
 * stored in console_setting, with defaults for anything not saved.
 */
public final class SecuritySettings {

    public static final String PASSWORD_MIN_LENGTH = "password.min_length";
    public static final String PASSWORD_MIN_CLASSES = "password.min_classes";
    public static final String PASSWORD_HISTORY = "password.history";
    public static final String PASSWORD_MAX_AGE_DAYS = "password.max_age_days";
    public static final String LOCKOUT_ATTEMPTS = "lockout.attempts";
    public static final String LOCKOUT_MINUTES = "lockout.minutes";
    public static final String TWO_FACTOR_REQUIRED_FOR_ADMINS = "twofactor.require_admin";
    public static final String SESSION_TIMEOUT_MINUTES = "session.timeout_minutes";
    public static final String AUDIT_KEEP_DAYS = "audit.keep_days";

    /** Setting -> default, in the order the settings page shows them. */
    static final Map<String, String> DEFAULTS = new LinkedHashMap<String, String>();
    static {
        DEFAULTS.put(PASSWORD_MIN_LENGTH, "10");
        DEFAULTS.put(PASSWORD_MIN_CLASSES, "3");
        DEFAULTS.put(PASSWORD_HISTORY, "5");
        DEFAULTS.put(PASSWORD_MAX_AGE_DAYS, "90");
        DEFAULTS.put(LOCKOUT_ATTEMPTS, "5");
        DEFAULTS.put(LOCKOUT_MINUTES, "15");
        DEFAULTS.put(TWO_FACTOR_REQUIRED_FOR_ADMINS, "true");
        DEFAULTS.put(SESSION_TIMEOUT_MINUTES, "30");
        DEFAULTS.put(AUDIT_KEEP_DAYS, "365");
    }

    private static volatile Map<String, String> cached;

    private SecuritySettings() {
    }

    static ConsoleSecurityDAO dao() throws Exception {
        return (ConsoleSecurityDAO) AdminMainProcessor.core.dao.createDAO(ConsoleSecurityDAO.class);
    }

    public static String get(String name) {
        Map<String, String> settings = cached;
        if (settings == null) {
            settings = new LinkedHashMap<String, String>(DEFAULTS);
            try {
                settings.putAll(dao().getSettings());
            } catch (Exception e) {
                AdminMainProcessor.core.log.warn("Security settings unavailable, using defaults: "
                        + e.getMessage());
            }
            cached = settings;
        }
        String value = settings.get(name);
        return value != null ? value : DEFAULTS.get(name);
    }

    public static int getInt(String name) {
        try {
            return Integer.parseInt(get(name).trim());
        } catch (RuntimeException e) {
            return Integer.parseInt(DEFAULTS.get(name));
        }
    }

    public static boolean getBoolean(String name) {
        return "true".equalsIgnoreCase(get(name));
    }

    public static void save(Map<String, String> values) throws Exception {
        ConsoleSecurityDAO dao = dao();
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (DEFAULTS.containsKey(e.getKey())) {
                dao.saveSetting(e.getKey(), e.getValue());
            }
        }
        cached = null;
        LockOut.apply();
    }
}
