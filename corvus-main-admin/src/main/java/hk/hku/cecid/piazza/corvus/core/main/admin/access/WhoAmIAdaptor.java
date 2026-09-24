package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;

/**
 * The signed-in user and access level as JSON (/admin/access/whoami), for
 * the console pages to disable what the user can't use. Enforcement stays
 * with the AccessControlFilter.
 */
public class WhoAmIAdaptor extends HttpRequestAdaptor {

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        Object level = request.getAttribute(AccessControlFilter.ACCESS_LEVEL_ATTRIBUTE);
        String user = request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName();
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            response.getWriter().print("{\"user\":\"" + json(user) + "\",\"level\":\""
                    + json(level == null ? "" : level.toString()) + "\"}");
        } catch (IOException e) {
            throw new RequestListenerException("Unable to answer whoami", e);
        }
        return null;
    }

    private static String json(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
