package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestEvent;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestFilter;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Enforces the access levels on every admin console request (registered
 * on the admin dispatcher): a disabled user is let in nowhere, a user who
 * must change the password is sent to My Account, and anything else needs
 * the level AccessRules gives it.
 */
public class AccessControlFilter implements HttpRequestFilter {

    public boolean requestAccepted(HttpRequestEvent event) {
        HttpServletRequest request = event.getRequest();
        HttpServletResponse response = event.getResponse();
        if (request.getUserPrincipal() == null) {
            // not an authenticated request: the container's constraint decides
            return true;
        }
        String path = event.getPathInfo();
        try {
            if (request.isUserInRole(AccessLevel.ROLE_DISABLED)) {
                deny(request, response, "Your account is disabled.");
                return false;
            }
            if (request.isUserInRole(AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED)
                    && !AccessRules.ACCOUNT_PATH.equals(path)) {
                response.sendRedirect(request.getContextPath() + "/admin" + AccessRules.ACCOUNT_PATH);
                return false;
            }
            AccessLevel level = AccessLevel.of(request);
            AccessLevel required = AccessRules.required(path, request);
            if (level == null || !level.includes(required)) {
                AdminMainProcessor.core.log.info("Access denied: " + request.getUserPrincipal().getName()
                        + " (" + (level == null ? "no level" : level.label) + ") "
                        + request.getMethod() + " " + path + " needs " + required.label);
                deny(request, response, "This needs the " + required.label + " access level; you are "
                        + (level == null ? "not assigned one" : "a " + level.label) + ".");
                return false;
            }
            return true;
        } catch (IOException e) {
            AdminMainProcessor.core.log.error("Unable to answer a denied request", e);
            return false;
        }
    }

    public void requestProcessed(HttpRequestEvent event) {
    }

    private void deny(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        String home = request.getContextPath() + "/admin/home";
        out.print("<!DOCTYPE html><html><head><title>Access denied</title></head>"
                + "<body style=\"font-family:sans-serif;margin:40px;\"><h2>Access denied</h2><p>"
                + escape(reason) + "</p><p><a href=\"" + home + "\">Back to the admin console</a></p>"
                + "</body></html>");
        response.flushBuffer();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
