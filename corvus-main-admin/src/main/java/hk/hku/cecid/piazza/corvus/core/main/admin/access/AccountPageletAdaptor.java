package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * Access > My Account: every signed-in user's access level, password
 * change (under the password policy) and two-factor sign-in setup. The
 * AccessControlFilter keeps a user here while the password must be changed
 * (first sign-in, reset, expired) or, for an Administrator who must use it,
 * until two-factor sign-in is set up.
 */
public class AccountPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/account", "");
        String username = request.getUserPrincipal().getName();
        UserStore.User user = null;
        try {
            user = new UserStore().find(username);
        } catch (Exception e) {
            // no JMX user database: fall back to the container's roles
        }

        String action = request.getParameter("request_action");
        if ("post".equalsIgnoreCase(request.getMethod()) && action != null) {
            try {
                String message;
                if ("change_password".equals(action)) {
                    message = changePassword(request, username);
                } else if ("2fa_start".equals(action)) {
                    Accounts.startTwoFactor(username);
                    message = "Scan the QR code with your authenticator app, then enter the code it shows.";
                } else if ("2fa_confirm".equals(action)) {
                    if (!Accounts.confirmTwoFactor(username, request.getParameter("code"))) {
                        throw new IllegalArgumentException("that code is not right; try the current one");
                    }
                    AuditLog.record(request, "two-factor set up", username, AuditLog.OK, null);
                    message = "Two-factor sign-in is on: you'll be asked for a code at each sign-in.";
                } else if ("2fa_disable".equals(action)) {
                    AccessLevel level = user != null ? user.level() : AccessLevel.of(request);
                    if (Accounts.twoFactorRequired(level)) {
                        throw new IllegalArgumentException("Administrators must use two-factor sign-in");
                    }
                    if (!Accounts.verifyTwoFactor(username, request.getParameter("code"))) {
                        throw new IllegalArgumentException("that code is not right");
                    }
                    Accounts.resetTwoFactor(username);
                    AuditLog.record(request, "two-factor turned off", username, AuditLog.OK, null);
                    message = "Two-factor sign-in is off.";
                } else {
                    throw new IllegalArgumentException("unknown action");
                }
                request.setAttribute(ATTR_MESSAGE, message);
                user = new UserStore().find(username);
            } catch (IllegalArgumentException | IllegalStateException e) {
                request.setAttribute(ATTR_MESSAGE, "Not changed: " + e.getMessage());
                if ("change_password".equals(action)) {
                    AuditLog.record(request, "password change", username, AuditLog.FAILED, e.getMessage());
                }
            } catch (Exception e) {
                AdminMainProcessor.core.log.error("Unable to update the account of " + username, e);
                request.setAttribute(ATTR_MESSAGE, "Unable to update the account: " + e.getMessage());
            }
        }

        AccessLevel level = user != null ? user.level() : AccessLevel.of(request);
        boolean twoFactor = Accounts.twoFactorEnabled(username);
        dom.setProperty("username", username);
        dom.setProperty("level", level == null ? "" : level.label);
        dom.setProperty("must_change", String.valueOf(user != null && user.mustChangePassword()));
        dom.setProperty("expired", String.valueOf(Accounts.passwordExpired(username)));
        Timestamp changed = Accounts.passwordChangedAt(username);
        dom.setProperty("password_changed", changed == null ? "" : changed.toString().replaceAll("\\.\\d+$", ""));
        dom.setProperty("policy", PasswordPolicy.describe());
        dom.setProperty("two_factor", String.valueOf(twoFactor));
        dom.setProperty("two_factor_required", String.valueOf(Accounts.twoFactorRequired(level)));
        String pending = twoFactor ? null : Accounts.pendingSecret(username);
        if (pending != null) {
            String uri = Totp.uri("Hermes2+ (" + request.getServerName() + ")", username, pending);
            dom.setProperty("enrol/secret", pending.replaceAll("(.{4})", "$1 ").trim());
            dom.setProperty("enrol/qr", QrCodeImage.dataUri(uri));
        }
        return dom.getSource();
    }

    private String changePassword(HttpServletRequest request, String username) throws Exception {
        String current = request.getParameter("current_password");
        String password = request.getParameter("new_password");
        if (password == null || !password.equals(request.getParameter("confirm_password"))) {
            throw new IllegalArgumentException("the new password and its confirmation differ");
        }
        UserStore store = new UserStore();
        if (!store.checkPassword(username, current)) {
            throw new IllegalArgumentException("the current password is wrong");
        }
        String problem = PasswordPolicy.check(password, username, Accounts.history(username));
        if (problem == null && password.equals(current)) {
            problem = "choose a password different from the current one";
        }
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        Accounts.passwordChanged(username, store.setPassword(username, password, false));
        AuditLog.record(request, "password change", username, AuditLog.OK, null);
        return "Password changed. Use it the next time you sign in.";
    }
}
