package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

/**
 * Access > Security Settings (Administrator): password policy, sign-in
 * lock-out, two-factor requirement, session timeout, audit log retention.
 */
public class SecuritySettingsPageletAdaptor extends AdminPageletAdaptor {

    /** name -> {label, min, max}; min/max null for yes/no settings. */
    private static final Map<String, Object[]> FIELDS = new LinkedHashMap<String, Object[]>();
    static {
        FIELDS.put(SecuritySettings.PASSWORD_MIN_LENGTH, new Object[] { "Minimum password length", 8, 128 });
        FIELDS.put(SecuritySettings.PASSWORD_MIN_CLASSES, new Object[] {
                "Character kinds required (of lower, upper, digits, other)", 1, 4 });
        FIELDS.put(SecuritySettings.PASSWORD_HISTORY, new Object[] { "Previous passwords that can't be reused", 1, 24 });
        FIELDS.put(SecuritySettings.PASSWORD_MAX_AGE_DAYS, new Object[] { "Password expires after (days, 0 = never)", 0, 3650 });
        FIELDS.put(SecuritySettings.LOCKOUT_ATTEMPTS, new Object[] { "Lock a user after this many failed sign-ins", 3, 50 });
        FIELDS.put(SecuritySettings.LOCKOUT_MINUTES, new Object[] { "Lock for (minutes)", 1, 1440 });
        FIELDS.put(SecuritySettings.TWO_FACTOR_REQUIRED_FOR_ADMINS, new Object[] {
                "Administrators must use two-factor sign-in", null, null });
        FIELDS.put(SecuritySettings.SESSION_TIMEOUT_MINUTES, new Object[] { "Sign out after inactivity (minutes)", 5, 720 });
        FIELDS.put(SecuritySettings.AUDIT_KEEP_DAYS, new Object[] { "Keep the audit log for (days, 0 = forever)", 0, 3650 });
    }

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/security", "");
        if ("post".equalsIgnoreCase(request.getMethod())) {
            try {
                Map<String, String> values = read(request);
                SecuritySettings.save(values);
                AuditLog.record(request, "security settings", null, AuditLog.OK, values.toString());
                request.setAttribute(ATTR_MESSAGE, "Security settings saved");
            } catch (IllegalArgumentException e) {
                request.setAttribute(ATTR_MESSAGE, "Not saved: " + e.getMessage());
            } catch (Exception e) {
                AdminMainProcessor.core.log.error("Unable to save the security settings", e);
                request.setAttribute(ATTR_MESSAGE, "Unable to save the security settings: " + e.getMessage());
            }
        }
        int i = 0;
        for (Map.Entry<String, Object[]> f : FIELDS.entrySet()) {
            String prefix = "setting[" + (++i) + "]/";
            dom.setProperty(prefix + "name", f.getKey());
            dom.setProperty(prefix + "label", (String) f.getValue()[0]);
            dom.setProperty(prefix + "value", SecuritySettings.get(f.getKey()));
            dom.setProperty(prefix + "type", f.getValue()[1] == null ? "boolean" : "number");
            if (f.getValue()[1] != null) {
                dom.setProperty(prefix + "min", String.valueOf(f.getValue()[1]));
                dom.setProperty(prefix + "max", String.valueOf(f.getValue()[2]));
            }
        }
        return dom.getSource();
    }

    private Map<String, String> read(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object[]> f : FIELDS.entrySet()) {
            String raw = request.getParameter(f.getKey());
            if (f.getValue()[1] == null) {
                values.put(f.getKey(), String.valueOf("true".equals(raw)));
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(raw.trim());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(f.getValue()[0] + " must be a number");
            }
            int min = (Integer) f.getValue()[1], max = (Integer) f.getValue()[2];
            if (value < min || value > max) {
                throw new IllegalArgumentException(f.getValue()[0] + " must be " + min + " to " + max);
            }
            values.put(f.getKey(), String.valueOf(value));
        }
        return values;
    }
}
