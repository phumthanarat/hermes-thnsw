package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import javax.servlet.http.HttpServletRequest;

/**
 * The admin console's access levels, each a Tomcat role. Higher levels can
 * do everything lower ones can.
 */
public enum AccessLevel {

    /** Views and searches only. */
    VIEWER("viewer", "Viewer"),
    /** Also runs day-to-day operations: Ping, delete/resend messages. */
    OPERATOR("operator", "Operator"),
    /** Everything, including settings and user management. */
    ADMINISTRATOR("admin", "Administrator");

    /** A user who must change the password before using the console. */
    public static final String ROLE_PASSWORD_CHANGE_REQUIRED = "password_change_required";

    /** A user who is disabled: authenticated but let in nowhere. */
    public static final String ROLE_DISABLED = "disabled";

    /** The pre-existing admin role, still honoured as Administrator. */
    static final String LEGACY_ADMIN_ROLE = "corvus";

    public final String role;
    public final String label;

    AccessLevel(String role, String label) {
        this.role = role;
        this.label = label;
    }

    public boolean includes(AccessLevel other) {
        return compareTo(other) >= 0;
    }

    /** @return the signed-in user's level, or null if none. */
    public static AccessLevel of(HttpServletRequest request) {
        if (request.isUserInRole(ADMINISTRATOR.role) || request.isUserInRole(LEGACY_ADMIN_ROLE)) {
            return ADMINISTRATOR;
        }
        if (request.isUserInRole(OPERATOR.role)) {
            return OPERATOR;
        }
        if (request.isUserInRole(VIEWER.role)) {
            return VIEWER;
        }
        return null;
    }

    /** @return the level a set of role names grants, or null if none. */
    public static AccessLevel ofRoles(java.util.Collection<String> roles) {
        if (roles.contains(ADMINISTRATOR.role) || roles.contains(LEGACY_ADMIN_ROLE)) {
            return ADMINISTRATOR;
        }
        if (roles.contains(OPERATOR.role)) {
            return OPERATOR;
        }
        if (roles.contains(VIEWER.role)) {
            return VIEWER;
        }
        return null;
    }

    public static AccessLevel forRole(String role) {
        for (AccessLevel level : values()) {
            if (level.role.equals(role)) {
                return level;
            }
        }
        return null;
    }
}
