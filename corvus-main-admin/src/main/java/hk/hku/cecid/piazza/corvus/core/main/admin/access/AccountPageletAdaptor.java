package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * Access > My Account: every signed-in user can see their access level and
 * change their own password. A user who must change the password (e.g. the
 * first admin/admin sign-in) is kept on this page until they do.
 */
public class AccountPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/account", "");
        String username = request.getUserPrincipal().getName();
        boolean mustChange = mustChange(request, username);
        boolean changed = false;
        try {
            if ("post".equalsIgnoreCase(request.getMethod())) {
                try {
                    changePassword(request, username);
                    changed = true;
                    mustChange = false;
                    request.setAttribute(ATTR_MESSAGE, "Password changed. Use it the next time you sign in.");
                } catch (IllegalArgumentException e) {
                    request.setAttribute(ATTR_MESSAGE, "Not changed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to change the password of " + username, e);
            request.setAttribute(ATTR_MESSAGE, "Unable to change the password: " + e.getMessage());
        }
        AccessLevel level = AccessLevel.of(request);
        try {
            UserStore.User user = new UserStore().find(username);
            if (user != null) {
                level = user.level();
            }
        } catch (Exception e) {
            // keep the level the container reports
        }
        dom.setProperty("username", username);
        dom.setProperty("level", level == null ? "" : level.label);
        dom.setProperty("must_change", String.valueOf(mustChange));
        dom.setProperty("changed", String.valueOf(changed));
        dom.setProperty("min_password_length", String.valueOf(UsersPageletAdaptor.MIN_PASSWORD_LENGTH));
        return dom.getSource();
    }

    private static boolean mustChange(HttpServletRequest request, String username) {
        try {
            UserStore.User user = new UserStore().find(username);
            return user != null && user.mustChangePassword();
        } catch (Exception e) {
            return request.isUserInRole(AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED);
        }
    }

    private void changePassword(HttpServletRequest request, String username) throws Exception {
        String current = request.getParameter("current_password");
        String password = request.getParameter("new_password");
        if (password == null || !password.equals(request.getParameter("confirm_password"))) {
            throw new IllegalArgumentException("the new password and its confirmation differ");
        }
        UserStore store = new UserStore();
        if (!store.checkPassword(username, current)) {
            throw new IllegalArgumentException("the current password is wrong");
        }
        if (password.equals(current)) {
            throw new IllegalArgumentException("choose a password different from the current one");
        }
        store.setPassword(username, UsersPageletAdaptor.password(password, username), false);
        AdminMainProcessor.core.log.info("Users: " + username + " changed their password");
    }
}
