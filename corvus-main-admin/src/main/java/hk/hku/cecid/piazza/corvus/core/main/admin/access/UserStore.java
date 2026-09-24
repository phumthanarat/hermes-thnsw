package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The console users, kept in Tomcat's UserDatabase (tomcat-users.xml) and
 * reached through its JMX MBeans, so a change is seen by the realm at once
 * and saved to the file. Each user holds one access level role, plus
 * optionally "password_change_required" and "disabled".
 */
public class UserStore {

    /** A user as the Users page shows it. */
    public static class User {
        public final String username;
        public final List<String> roles;

        User(String username, List<String> roles) {
            this.username = username;
            this.roles = roles;
        }

        public AccessLevel level() {
            return AccessLevel.ofRoles(roles);
        }

        public boolean isDisabled() {
            return roles.contains(AccessLevel.ROLE_DISABLED);
        }

        public boolean mustChangePassword() {
            return roles.contains(AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED);
        }

        /** A console user, as opposed to e.g. the REST API user. */
        public boolean isConsoleUser() {
            return level() != null;
        }
    }

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private final ObjectName database;

    public UserStore() throws Exception {
        Set<ObjectName> found = server.queryNames(new ObjectName("Users:type=UserDatabase,*"), null);
        if (found.isEmpty()) {
            throw new IllegalStateException("Tomcat's UserDatabase is not available over JMX");
        }
        database = found.iterator().next();
    }

    public List<User> list() throws Exception {
        List<User> users = new ArrayList<User>();
        for (String name : (String[]) server.getAttribute(database, "users")) {
            ObjectName user = new ObjectName(name);
            users.add(new User((String) server.getAttribute(user, "username"), roles(user)));
        }
        Collections.sort(users, (a, b) -> a.username.compareToIgnoreCase(b.username));
        return users;
    }

    public User find(String username) throws Exception {
        ObjectName user = user(username);
        return user == null ? null : new User(username, roles(user));
    }

    /** @return the stored password digest. */
    public String create(String username, String password, AccessLevel level,
            boolean mustChangePassword) throws Exception {
        if (user(username) != null) {
            throw new IllegalArgumentException("User " + username + " already exists");
        }
        String digest = PasswordDigest.mutate(password);
        server.invoke(database, "createUser", new Object[] { username, digest, "" },
                new String[] { String.class.getName(), String.class.getName(), String.class.getName() });
        setRoles(username, level, mustChangePassword, false);
        return digest;
    }

    public void remove(String username) throws Exception {
        server.invoke(database, "removeUser", new Object[] { username },
                new String[] { String.class.getName() });
        save();
    }

    public boolean checkPassword(String username, String password) throws Exception {
        ObjectName user = user(username);
        return user != null
                && PasswordDigest.matches(password, (String) server.getAttribute(user, "password"));
    }

    /** @return the stored password digest. */
    public String setPassword(String username, String password, boolean mustChangePassword)
            throws Exception {
        ObjectName user = requireUser(username);
        String digest = PasswordDigest.mutate(password);
        server.setAttribute(user, new Attribute("password", digest));
        User current = find(username);
        setRoles(username, current.level(), mustChangePassword, current.isDisabled());
        return digest;
    }

    /** Replaces the user's console roles, keeping any others (e.g. "user"). */
    public void setRoles(String username, AccessLevel level, boolean mustChangePassword,
            boolean disabled) throws Exception {
        ObjectName user = requireUser(username);
        List<String> managed = new ArrayList<String>(Arrays.asList(
                AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED, AccessLevel.ROLE_DISABLED,
                AccessLevel.LEGACY_ADMIN_ROLE));
        for (AccessLevel l : AccessLevel.values()) {
            managed.add(l.role);
        }
        for (String role : roles(user)) {
            if (managed.contains(role)) {
                invokeOnUser(user, "removeRole", role);
            }
        }
        addRole(user, level.role);
        if (mustChangePassword) {
            addRole(user, AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED);
        }
        if (disabled) {
            addRole(user, AccessLevel.ROLE_DISABLED);
        }
        save();
    }

    private void addRole(ObjectName user, String role) throws Exception {
        // a user can only hold a role the database defines
        Object existing = server.invoke(database, "findRole", new Object[] { role },
                new String[] { String.class.getName() });
        if (existing == null) {
            server.invoke(database, "createRole", new Object[] { role, "" },
                    new String[] { String.class.getName(), String.class.getName() });
        }
        invokeOnUser(user, "addRole", role);
    }

    private void invokeOnUser(ObjectName user, String operation, String role) throws Exception {
        server.invoke(user, operation, new Object[] { role }, new String[] { String.class.getName() });
    }

    private void save() throws Exception {
        server.invoke(database, "save", new Object[0], new String[0]);
    }

    private List<String> roles(ObjectName user) throws Exception {
        List<String> roles = new ArrayList<String>();
        for (String role : (String[]) server.getAttribute(user, "roles")) {
            // the MBean may report roles as object names ("...rolename=x...")
            int at = role.indexOf("rolename=");
            if (at >= 0) {
                String rest = role.substring(at + "rolename=".length());
                role = rest.startsWith("\"") ? rest.substring(1, rest.indexOf('"', 1))
                        : rest.replaceAll(",.*$", "");
            }
            roles.add(role);
        }
        return roles;
    }

    private ObjectName user(String username) throws Exception {
        Object name = server.invoke(database, "findUser", new Object[] { username },
                new String[] { String.class.getName() });
        return name == null ? null : new ObjectName((String) name);
    }

    private ObjectName requireUser(String username) throws Exception {
        ObjectName user = user(username);
        if (user == null) {
            throw new IllegalArgumentException("No such user: " + username);
        }
        return user;
    }
}
