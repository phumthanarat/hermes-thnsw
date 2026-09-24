package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

/**
 * The admin console's sign-in page (/admin/login): username and password,
 * checked by the container realm (request.login, so the same users,
 * digests and lock-out apply), then - for users with two-factor sign-in -
 * a code from their authenticator app. The user is kept in the session
 * until sign-out or the session times out.
 */
public class SignInAdaptor extends HttpRequestAdaptor {

    /** Session: the password is right but the two-factor code is still due. */
    static final String TWO_FACTOR_PENDING = "hermes.access.2fa_pending";
    static final String TWO_FACTOR_FAILURES = "hermes.access.2fa_failures";
    static final String NEXT = "hermes.access.next";
    static final int MAX_CODE_FAILURES = 5;

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        try {
            String home = request.getContextPath() + "/admin/home";
            HttpSession session = request.getSession(false);
            boolean pending = session != null && session.getAttribute(TWO_FACTOR_PENDING) != null
                    && request.getUserPrincipal() != null;

            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                if (pending) {
                    page(request, response, true, null, null);
                } else if (request.getUserPrincipal() != null && session != null) {
                    response.sendRedirect(home);
                } else {
                    String note = "1".equals(request.getParameter("signed_out")) ? "You have signed out."
                            : "1".equals(request.getParameter("expired")) ? "Your session has ended. Please sign in again."
                            : null;
                    page(request, response, false, null, note);
                }
                return null;
            }

            if ("code".equals(request.getParameter("step"))) {
                if (!pending) {
                    page(request, response, false, "Please sign in again.", null);
                    return null;
                }
                checkCode(request, response, session);
                return null;
            }
            checkPassword(request, response);
        } catch (IOException e) {
            AdminMainProcessor.core.log.error("Unable to answer the sign-in page", e);
        }
        return null;
    }

    private void checkPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = trim(request.getParameter("username"));
        String password = request.getParameter("password");
        String next = safeNext(request, request.getParameter("next"));
        signOut(request);
        HttpSession session = request.getSession(true);
        try {
            request.login(username == null ? "" : username, password == null ? "" : password);
        } catch (ServletException e) {
            boolean locked = username != null && LockOut.isLocked(username);
            AuditLog.record(request, username, "sign-in", null, AuditLog.FAILED,
                    locked ? "account locked out" : "wrong username or password");
            page(request, response, false, "Invalid username or password.", null);
            return;
        }

        UserStore.User user = null;
        try {
            user = new UserStore().find(username);
        } catch (Exception e) {
            // no JMX user database: rely on the container's roles
        }
        AccessLevel level = user != null ? user.level() : AccessLevel.of(request);
        if (level == null || (user != null && user.isDisabled())) {
            AuditLog.record(request, username, "sign-in", null, AuditLog.DENIED,
                    level == null ? "not a console user" : "account disabled");
            signOut(request);
            page(request, response, false, "This account cannot use the admin console.", null);
            return;
        }

        session = request.getSession(true);
        session.setMaxInactiveInterval(
                Math.max(1, SecuritySettings.getInt(SecuritySettings.SESSION_TIMEOUT_MINUTES)) * 60);
        if (Accounts.twoFactorEnabled(username)) {
            session.setAttribute(TWO_FACTOR_PENDING, Boolean.TRUE);
            session.setAttribute(NEXT, next);
            AuditLog.record(request, username, "sign-in", null, AuditLog.OK, "password accepted, code due");
            page(request, response, true, null, null);
            return;
        }
        AuditLog.record(request, username, "sign-in", null, AuditLog.OK, level.label);
        // an Administrator who must but hasn't set up two-factor sign-in is
        // held on My Account by the AccessControlFilter until they do
        response.sendRedirect(next);
    }

    private void checkCode(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        String username = request.getUserPrincipal().getName();
        boolean ok;
        try {
            ok = Accounts.verifyTwoFactor(username, request.getParameter("code"));
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to check the two-factor code of " + username, e);
            ok = false;
        }
        if (ok) {
            String next = (String) session.getAttribute(NEXT);
            session.removeAttribute(TWO_FACTOR_PENDING);
            session.removeAttribute(TWO_FACTOR_FAILURES);
            session.removeAttribute(NEXT);
            AuditLog.record(request, username, "sign-in", null, AuditLog.OK, "two-factor code accepted");
            response.sendRedirect(next != null ? next : request.getContextPath() + "/admin/home");
            return;
        }
        Integer failures = (Integer) session.getAttribute(TWO_FACTOR_FAILURES);
        failures = Integer.valueOf(failures == null ? 1 : failures.intValue() + 1);
        AuditLog.record(request, username, "sign-in", null, AuditLog.FAILED,
                "wrong two-factor code (" + failures + ")");
        if (failures.intValue() >= MAX_CODE_FAILURES) {
            signOut(request);
            page(request, response, false, "Too many wrong codes. Please sign in again.", null);
            return;
        }
        session.setAttribute(TWO_FACTOR_FAILURES, failures);
        page(request, response, true, "That code is not right. Try the current one.", null);
    }

    static void signOut(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            // as far as possible is enough
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Only ever return to a page of this console. */
    static String safeNext(HttpServletRequest request, String next) {
        String admin = request.getContextPath() + "/admin/";
        if (next == null || !next.startsWith(admin) || next.contains("//") || next.contains("\\")
                || next.startsWith(admin + "login") || next.startsWith(admin + "logout")) {
            return admin + "home";
        }
        return next;
    }

    private void page(HttpServletRequest request, HttpServletResponse response, boolean codeStep,
            String error, String note) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String action = request.getContextPath() + "/admin/login";
        StringBuilder form = new StringBuilder();
        if (codeStep) {
            form.append("<input type=\"hidden\" name=\"step\" value=\"code\">")
                .append("<p class=\"hint\">Enter the 6-digit code from your authenticator app.</p>")
                .append("<label for=\"code\">Code</label>")
                .append("<input type=\"text\" id=\"code\" name=\"code\" inputmode=\"numeric\" pattern=\"[0-9 ]{6,7}\"")
                .append(" autocomplete=\"one-time-code\" autofocus required>")
                .append("<button type=\"submit\">Verify</button>")
                .append("<p class=\"alt\"><a href=\"").append(request.getContextPath())
                .append("/admin/logout\">Cancel</a></p>");
        } else {
            form.append("<input type=\"hidden\" name=\"next\" value=\"")
                .append(escape(safeNext(request, request.getParameter("next")))).append("\">")
                .append("<label for=\"username\">Username</label>")
                .append("<input type=\"text\" id=\"username\" name=\"username\" autocomplete=\"username\" autofocus required value=\"")
                .append(escape(trim(request.getParameter("username")))).append("\">")
                .append("<label for=\"password\">Password</label>")
                .append("<input type=\"password\" id=\"password\" name=\"password\" autocomplete=\"current-password\" required>")
                .append("<button type=\"submit\">Sign in</button>");
        }
        PrintWriter out = response.getWriter();
        out.print("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Sign in - Hermes2+</title><style>"
                + ":root{--bg:#f5f7fb;--card:#fff;--text:#1f2937;--soft:#6b7280;--border:#e5e7eb;--accent:#2563eb;--danger:#b91c1c}"
                + "@media (prefers-color-scheme:dark){:root{--bg:#0f172a;--card:#111827;--text:#e5e7eb;--soft:#9ca3af;--border:#1f2937;--accent:#60a5fa;--danger:#f87171}}"
                + "*{box-sizing:border-box}body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
                + "background:var(--bg);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;padding:16px}"
                + ".card{width:100%;max-width:360px;background:var(--card);border:1px solid var(--border);border-radius:12px;padding:28px}"
                + ".brand{display:flex;align-items:center;gap:10px;margin-bottom:20px}"
                + ".mark{width:36px;height:36px;border-radius:8px;background:var(--accent);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700}"
                + ".name{font-weight:700}.tagline{font-size:12px;color:var(--soft)}"
                + "label{display:block;font-size:13px;margin:14px 0 6px}"
                + "input[type=text],input[type=password]{width:100%;padding:9px 10px;border:1px solid var(--border);border-radius:8px;background:var(--card);color:var(--text);font-size:14px}"
                + "button{width:100%;margin-top:20px;padding:10px;border:0;border-radius:8px;background:var(--accent);color:#fff;font-size:14px;font-weight:600;cursor:pointer}"
                + ".note{font-size:13px;margin:0 0 6px;padding:8px 10px;border-radius:8px;border:1px solid var(--border)}"
                + ".error{color:var(--danger)}.hint{font-size:13px;color:var(--soft);margin:0}.alt{text-align:center;font-size:13px;margin:14px 0 0}"
                + ".alt a{color:var(--soft)}</style></head><body>"
                + "<form class=\"card\" method=\"post\" action=\"" + escape(action) + "\">"
                + "<div class=\"brand\"><div class=\"mark\">H2</div><div><div class=\"name\">Hermes2+</div>"
                + "<div class=\"tagline\">Administration Console</div></div></div>"
                + (error == null ? "" : "<p class=\"note error\">" + escape(error) + "</p>")
                + (note == null ? "" : "<p class=\"note\">" + escape(note) + "</p>")
                + form + "</form></body></html>");
        out.flush();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
