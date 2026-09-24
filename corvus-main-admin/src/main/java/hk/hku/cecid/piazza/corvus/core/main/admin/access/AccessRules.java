package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import javax.servlet.http.HttpServletRequest;

/**
 * Which access level each admin console request needs. Paths are relative
 * to /admin (e.g. "/ebms/ping").
 * <ul>
 * <li>Viewer: every page, and the searches, views and downloads that
 * happen to be POSTed (message history, repository, document files).</li>
 * <li>Operator: also the day-to-day operations - Ping, deleting,
 * resending and re-queuing messages, the HTTP ping test.</li>
 * <li>Administrator: anything else that changes something (settings,
 * partnerships, agreements, housekeeping, certificates, API keys, ...) and
 * the pages listing users or API keys.</li>
 * </ul>
 * A change not listed here needs Administrator, so a new admin page is
 * safe by default.
 */
public final class AccessRules {

    /** Reachable by every signed-in user, even one who must change the password. */
    public static final String ACCOUNT_PATH = "/access/account";

    private AccessRules() {
    }

    public static AccessLevel required(String path, HttpServletRequest request) {
        if (path == null) {
            path = "";
        }
        String action = request.getParameter("action");
        boolean post = "POST".equalsIgnoreCase(request.getMethod());

        // pages that show secrets or users
        if (path.startsWith("/access/users") || path.startsWith("/api/keys")) {
            return AccessLevel.ADMINISTRATOR;
        }
        // actions some pages take on any method, GET included
        if (path.equals("/home") && ("gc".equalsIgnoreCase(action) || "final".equalsIgnoreCase(action))) {
            return AccessLevel.ADMINISTRATOR;
        }
        if (path.equals("/main/httpd") && action != null) {
            return "ping_test".equals(action) ? AccessLevel.OPERATOR : AccessLevel.ADMINISTRATOR;
        }
        if (!post) {
            return AccessLevel.VIEWER;
        }

        if (path.equals(ACCOUNT_PATH)) {
            return AccessLevel.VIEWER;
        }
        // POSTed but read-only
        if (path.endsWith("/message_history")) {
            return "delete".equals(request.getParameter("request_action"))
                    ? AccessLevel.OPERATOR : AccessLevel.VIEWER;
        }
        if (path.endsWith("/repository") || path.equals("/ebms/document_reference")
                || path.equals("/ebms/document_export")) {
            return AccessLevel.VIEWER;
        }
        // operations
        if (path.equals("/ebms/ping")) {
            return request.getParameter("save_parties") != null
                    ? AccessLevel.ADMINISTRATOR : AccessLevel.OPERATOR;
        }
        if (path.endsWith("/change_message_status") || path.endsWith("/resend_as_new")) {
            return AccessLevel.OPERATOR;
        }
        return AccessLevel.ADMINISTRATOR;
    }
}
