package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;

/** Signs out of the admin console (/admin/logout). */
public class SignOutAdaptor extends HttpRequestAdaptor {

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        if (request.getUserPrincipal() != null) {
            AuditLog.record(request, "sign-out", null, AuditLog.OK, null);
        }
        SignInAdaptor.signOut(request);
        try {
            response.sendRedirect(request.getContextPath() + "/admin/login?signed_out=1");
        } catch (IOException e) {
            throw new RequestListenerException("Unable to sign out", e);
        }
        return null;
    }
}
