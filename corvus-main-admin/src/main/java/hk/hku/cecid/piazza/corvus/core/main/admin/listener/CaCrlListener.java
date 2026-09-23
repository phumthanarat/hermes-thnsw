package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.ca.CertificateAuthority;

/**
 * CaCrlListener serves the current CRL for this gateway's internal CA as
 * DER-encoded bytes -- unauthenticated, like the AS2/ebMS/SFRM inbound
 * endpoints, since any partner (or this gateway's own {@link
 * hk.hku.cecid.piazza.commons.security.RevocationChecker}) needs to be able
 * to fetch it without admin credentials. Registered on the plain
 * core httpd.listener extension point rather than as an admin pagelet,
 * since a CRL is binary, not an HTML page rendered through the admin shell.
 */
public class CaCrlListener extends HttpRequestAdaptor {

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        try {
            byte[] crl = CertificateAuthority.buildCrl();
            response.setContentType("application/pkix-crl");
            response.setContentLength(crl.length);
            response.getOutputStream().write(crl);
            response.getOutputStream().flush();
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to build CRL", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to build CRL");
            } catch (Exception ignore) {
            }
        }
        return null;
    }
}
