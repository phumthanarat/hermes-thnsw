package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestEvent;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestFilter;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Guards every admin console request (registered on the admin dispatcher):
 * <ol>
 * <li>optionally requires HTTPS (system property hermes.admin.requireHttps,
 * redirecting to port hermes.admin.httpsPort);</li>
 * <li>sends a request without a signed-in user to the sign-in page, or signs
 * in a script's Basic credentials for that request alone;</li>
 * <li>rejects a cross-site form submission (Origin/Referer of another host)
 * made with a signed-in session;</li>
 * <li>lets a disabled or deleted user in nowhere, keeps a user who must
 * change the password on My Account, and requires the access level
 * AccessRules gives the request.</li>
 * </ol>
 * Roles are read live from the user database, so a change an administrator
 * makes applies to a signed-in user straight away.
 */
public class AccessControlFilter implements HttpRequestFilter {

    public static final String REQUIRE_HTTPS_PROPERTY = "hermes.admin.requireHttps";
    public static final String HTTPS_PORT_PROPERTY = "hermes.admin.httpsPort";

    public boolean requestAccepted(HttpRequestEvent event) {
        HttpServletRequest request = event.getRequest();
        HttpServletResponse response = event.getResponse();
        String path = event.getPathInfo();
        try {
            if (Boolean.getBoolean(REQUIRE_HTTPS_PROPERTY) && !request.isSecure()) {
                response.sendRedirect(httpsUrl(request));
                return false;
            }

            boolean basic = false;
            if (request.getUserPrincipal() == null) {
                basic = signInBasic(request);
                if (!basic) {
                    if (request.getHeader("Authorization") != null) {
                        // a script with wrong credentials: answer it as such
                        response.setHeader("WWW-Authenticate", "Basic realm=\"Corvus Restricted Area\"");
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        response.sendRedirect(signInUrl(request));
                    }
                    return false;
                }
            }

            String username = request.getUserPrincipal().getName();
            if (!basic && "POST".equalsIgnoreCase(request.getMethod()) && isCrossSite(request)) {
                AdminMainProcessor.core.log.warn("Access denied: cross-site POST " + path
                        + " for " + username + " from " + request.getHeader("Origin")
                        + " / " + request.getHeader("Referer"));
                deny(request, response, "This form was submitted from another site.");
                return false;
            }
            // the roles cached in the session date from sign-in: use the
            // user's current ones, so changes apply at once
            Collection<String> roles = currentRoles(request, username);
            if (roles == null || roles.contains(AccessLevel.ROLE_DISABLED)) {
                signOut(request);
                deny(request, response, "Your account is disabled or no longer exists.");
                return false;
            }
            if (roles.contains(AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED)
                    && !AccessRules.ACCOUNT_PATH.equals(path)) {
                response.sendRedirect(request.getContextPath() + "/admin" + AccessRules.ACCOUNT_PATH);
                return false;
            }
            AccessLevel level = AccessLevel.ofRoles(roles);
            AccessLevel required = AccessRules.required(path, request);
            if (level == null || !level.includes(required)) {
                AdminMainProcessor.core.log.info("Access denied: " + username + " ("
                        + (level == null ? "no level" : level.label) + ") " + request.getMethod()
                        + " " + path + " needs " + required.label);
                deny(request, response, "This needs the " + required.label + " access level; you are "
                        + (level == null ? "not assigned one" : "a " + level.label) + ".");
                return false;
            }
            return true;
        } catch (IOException e) {
            AdminMainProcessor.core.log.error("Unable to answer an admin request", e);
            return false;
        }
    }

    public void requestProcessed(HttpRequestEvent event) {
    }

    /** Signs in a script's Basic credentials for this request only. */
    private boolean signInBasic(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return false;
            }
            request.login(decoded.substring(0, colon), decoded.substring(colon + 1));
            return true;
        } catch (IllegalArgumentException | ServletException e) {
            return false;
        }
    }

    /** A POST whose Origin (or else Referer) names another host:port. */
    static boolean isCrossSite(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null || "null".equals(source)) {
            source = request.getHeader("Referer");
        }
        if (source == null) {
            // browsers send one of them on form posts; tools may send neither
            return false;
        }
        String host = request.getHeader("Host");
        try {
            URI from = new URI(source);
            URI to = new URI((request.isSecure() ? "https" : "http") + "://" + host);
            return from.getHost() == null || to.getHost() == null
                    || !from.getHost().equalsIgnoreCase(to.getHost())
                    || port(from) != port(to);
        } catch (Exception e) {
            return true;
        }
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /**
     * @return the user's roles in the user database now, null if the user no
     *         longer exists; without a JMX user database (another realm),
     *         those the container reports.
     */
    private Collection<String> currentRoles(HttpServletRequest request, String username) {
        try {
            UserStore.User user = new UserStore().find(username);
            return user == null ? null : user.roles;
        } catch (Exception e) {
            List<String> roles = new ArrayList<String>();
            for (String role : new String[] { AccessLevel.ADMINISTRATOR.role, AccessLevel.LEGACY_ADMIN_ROLE,
                    AccessLevel.OPERATOR.role, AccessLevel.VIEWER.role, AccessLevel.ROLE_DISABLED,
                    AccessLevel.ROLE_PASSWORD_CHANGE_REQUIRED }) {
                if (request.isUserInRole(role)) {
                    roles.add(role);
                }
            }
            return roles;
        }
    }

    private void signOut(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            // signing out as far as possible is enough here
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static String signInUrl(HttpServletRequest request) throws IOException {
        String next = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getQueryString() != null) {
            next += "?" + request.getQueryString();
        }
        return request.getContextPath() + "/login.jsp?next=" + URLEncoder.encode(next, "UTF-8");
    }

    private static String httpsUrl(HttpServletRequest request) {
        String port = System.getProperty(HTTPS_PORT_PROPERTY, "443");
        String url = "https://" + request.getServerName() + ("443".equals(port) ? "" : ":" + port)
                + request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }
        return url;
    }

    private void deny(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        String home = request.getContextPath() + "/admin/home";
        out.print("<!DOCTYPE html><html><head><title>Access denied</title></head>"
                + "<body style=\"font-family:sans-serif;margin:40px;\"><h2>Access denied</h2><p>"
                + escape(reason) + "</p><p><a href=\"" + home + "\">Back to the admin console</a>"
                + " &middot; <a href=\"" + request.getContextPath() + "/logout.jsp\">Sign out</a></p>"
                + "</body></html>");
        response.flushBuffer();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
