package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * Access > Users (Administrator only, see AccessRules): add and delete
 * console users, set their access level, reset passwords, disable them.
 * There is always at least one enabled Administrator, and nobody can
 * delete or disable themselves.
 */
public class UsersPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/users", "");
        try {
            UserStore store = new UserStore();
            if ("post".equalsIgnoreCase(request.getMethod())) {
                String action = request.getParameter("request_action");
                String target = request.getParameter("username");
                try {
                    String result = handle(request, store);
                    AuditLog.record(request, "users: " + action, target, AuditLog.OK, result);
                    request.setAttribute(ATTR_MESSAGE, result);
                } catch (IllegalArgumentException e) {
                    AuditLog.record(request, "users: " + action, target, AuditLog.FAILED, e.getMessage());
                    request.setAttribute(ATTR_MESSAGE, "Not changed: " + e.getMessage());
                }
            }
            render(store, request, dom);
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to process the users page", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to manage users: " + e.getMessage());
        }
        return dom.getSource();
    }

    private String handle(HttpServletRequest request, UserStore store) throws Exception {
        String action = request.getParameter("request_action");
        String username = trim(request.getParameter("username"));
        String self = request.getUserPrincipal().getName();
        if (username == null) {
            throw new IllegalArgumentException("choose a user");
        }

        if ("add".equals(action)) {
            if (!username.matches("[A-Za-z0-9._@-]{1,64}")) {
                throw new IllegalArgumentException("a username has 1-64 letters, digits or . _ @ -");
            }
            AccessLevel level = level(request);
            String digest = store.create(username, password(request.getParameter("password"), username, null),
                    level, request.getParameter("must_change") != null);
            Accounts.passwordChanged(username, digest);
            return "User " + username + " added as " + level.label;
        }

        UserStore.User user = store.find(username);
        if (user == null || !user.isConsoleUser()) {
            throw new IllegalArgumentException("no console user " + username);
        }
        if ("set_level".equals(action)) {
            AccessLevel level = level(request);
            if (level != AccessLevel.ADMINISTRATOR) {
                requireAnotherAdministrator(store, user);
            }
            store.setRoles(username, level, user.mustChangePassword(), user.isDisabled());
            return username + " is now " + level.label;
        }
        if ("reset_password".equals(action)) {
            boolean mustChange = request.getParameter("must_change") != null;
            String digest = store.setPassword(username,
                    password(request.getParameter("password"), username, Accounts.history(username)), mustChange);
            Accounts.passwordChanged(username, digest);
            return "Password of " + username + " reset"
                    + (mustChange ? "; it must be changed at next sign-in" : "");
        }
        if ("toggle_disabled".equals(action)) {
            if (username.equals(self)) {
                throw new IllegalArgumentException("you cannot disable yourself");
            }
            if (!user.isDisabled()) {
                requireAnotherAdministrator(store, user);
            }
            store.setRoles(username, user.level(), user.mustChangePassword(), !user.isDisabled());
            return username + (user.isDisabled() ? " enabled" : " disabled");
        }
        if ("delete".equals(action)) {
            if (username.equals(self)) {
                throw new IllegalArgumentException("you cannot delete yourself");
            }
            requireAnotherAdministrator(store, user);
            store.remove(username);
            Accounts.forget(username);
            return "User " + username + " deleted";
        }
        if ("unlock".equals(action)) {
            LockOut.unlock(username);
            return username + " unlocked";
        }
        if ("reset_2fa".equals(action)) {
            Accounts.resetTwoFactor(username);
            return "Two-factor sign-in of " + username + " reset: they set it up again"
                    + (Accounts.twoFactorRequired(user.level()) ? " at their next sign-in" : " if they want");
        }
        throw new IllegalArgumentException("unknown action");
    }

    /** Refuses to take away the last enabled Administrator. */
    private void requireAnotherAdministrator(UserStore store, UserStore.User user) throws Exception {
        if (user.level() != AccessLevel.ADMINISTRATOR || user.isDisabled()) {
            return;
        }
        for (UserStore.User other : store.list()) {
            if (!other.username.equals(user.username) && other.level() == AccessLevel.ADMINISTRATOR
                    && !other.isDisabled()) {
                return;
            }
        }
        throw new IllegalArgumentException(user.username + " is the last enabled Administrator");
    }

    private AccessLevel level(HttpServletRequest request) {
        AccessLevel level = AccessLevel.forRole(request.getParameter("level"));
        if (level == null) {
            throw new IllegalArgumentException("choose an access level");
        }
        return level;
    }

    /** @return the password if the password policy accepts it. */
    static String password(String password, String username, String[] history) {
        String problem = PasswordPolicy.check(password, username, history);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return password;
    }

    private void render(UserStore store, HttpServletRequest request, PropertyTree dom) throws Exception {
        List<UserStore.User> users = store.list();
        String self = request.getUserPrincipal().getName();
        int i = 0;
        for (UserStore.User user : users) {
            String prefix = "user[" + (++i) + "]/";
            dom.setProperty(prefix + "username", user.username);
            dom.setProperty(prefix + "console", String.valueOf(user.isConsoleUser()));
            dom.setProperty(prefix + "level", user.level() == null ? "" : user.level().role);
            dom.setProperty(prefix + "level_label", user.level() == null
                    ? String.join(", ", user.roles) : user.level().label);
            dom.setProperty(prefix + "disabled", String.valueOf(user.isDisabled()));
            dom.setProperty(prefix + "must_change", String.valueOf(user.mustChangePassword()));
            dom.setProperty(prefix + "self", String.valueOf(user.username.equals(self)));
            dom.setProperty(prefix + "locked", String.valueOf(LockOut.isLocked(user.username)));
            if (user.isConsoleUser()) {
                dom.setProperty(prefix + "two_factor", String.valueOf(Accounts.twoFactorEnabled(user.username)));
                java.sql.Timestamp changed = Accounts.passwordChangedAt(user.username);
                dom.setProperty(prefix + "password_changed",
                        changed == null ? "" : changed.toString().substring(0, 10));
            }
        }
        i = 0;
        for (AccessLevel level : AccessLevel.values()) {
            dom.setProperty("level[" + (++i) + "]/role", level.role);
            dom.setProperty("level[" + i + "]/label", level.label);
        }
        dom.setProperty("policy", PasswordPolicy.describe());
    }

    private static String trim(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }
}
