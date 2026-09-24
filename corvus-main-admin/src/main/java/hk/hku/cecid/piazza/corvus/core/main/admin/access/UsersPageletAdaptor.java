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

    static final int MIN_PASSWORD_LENGTH = 8;

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/users", "");
        try {
            UserStore store = new UserStore();
            if ("post".equalsIgnoreCase(request.getMethod())) {
                try {
                    request.setAttribute(ATTR_MESSAGE, handle(request, store));
                } catch (IllegalArgumentException e) {
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
            store.create(username, password(request.getParameter("password"), username), level,
                    request.getParameter("must_change") != null);
            log(self, "added " + username + " as " + level.label);
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
            log(self, "set " + username + " to " + level.label);
            return username + " is now " + level.label;
        }
        if ("reset_password".equals(action)) {
            boolean mustChange = request.getParameter("must_change") != null;
            store.setPassword(username, password(request.getParameter("password"), username), mustChange);
            log(self, "reset the password of " + username);
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
            log(self, (user.isDisabled() ? "enabled " : "disabled ") + username);
            return username + (user.isDisabled() ? " enabled" : " disabled");
        }
        if ("delete".equals(action)) {
            if (username.equals(self)) {
                throw new IllegalArgumentException("you cannot delete yourself");
            }
            requireAnotherAdministrator(store, user);
            store.remove(username);
            log(self, "deleted " + username);
            return "User " + username + " deleted";
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

    /** @return the password if it is acceptable. */
    static String password(String password, String username) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("a password needs at least " + MIN_PASSWORD_LENGTH
                    + " characters");
        }
        if (password.equalsIgnoreCase(username) || password.equalsIgnoreCase("admin")
                || password.equalsIgnoreCase("password")) {
            throw new IllegalArgumentException("that password is too easy to guess");
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
        }
        i = 0;
        for (AccessLevel level : AccessLevel.values()) {
            dom.setProperty("level[" + (++i) + "]/role", level.role);
            dom.setProperty("level[" + i + "]/label", level.label);
        }
        dom.setProperty("min_password_length", String.valueOf(MIN_PASSWORD_LENGTH));
    }

    private static void log(String by, String what) {
        AdminMainProcessor.core.log.info("Users: " + by + " " + what);
    }

    private static String trim(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }
}
