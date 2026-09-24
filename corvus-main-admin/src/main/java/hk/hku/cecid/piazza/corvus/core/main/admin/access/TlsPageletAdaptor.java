package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

/**
 * Main > HTTPS Certificate: the certificate the admin console and gateway
 * serve over HTTPS, and replacing it (PEM, PKCS#12, or a new self-signed
 * one) without a restart. Changes need the Administrator level.
 */
public class TlsPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/tls", "");
        String action = request.getParameter("request_action");
        if ("post".equalsIgnoreCase(request.getMethod()) && action != null) {
            try {
                TlsCertificate.Info installed;
                if ("pem".equals(action)) {
                    installed = TlsCertificate.installPem(nz(request.getParameter("certificate")),
                            nz(request.getParameter("private_key")), request.getParameter("key_password"));
                } else if ("pkcs12".equals(action)) {
                    String data = nz(request.getParameter("pkcs12_base64"));
                    if (data.isEmpty()) {
                        throw new IllegalArgumentException("choose a .p12 / .pfx file");
                    }
                    installed = TlsCertificate.installPkcs12(Base64.getDecoder().decode(
                            data.replaceAll("^data:[^,]*,", "").replaceAll("\\s", "")),
                            request.getParameter("pkcs12_password"));
                } else if ("self_signed".equals(action)) {
                    installed = TlsCertificate.generateSelfSigned(nz(request.getParameter("hostname")).trim());
                } else {
                    throw new IllegalArgumentException("unknown action");
                }
                AuditLog.record(request, "https certificate: " + action, installed.subject, AuditLog.OK,
                        "SHA-256 " + installed.fingerprint + ", valid to " + installed.notAfter);
                request.setAttribute(ATTR_MESSAGE, "Certificate installed and in use: " + installed.subject
                        + " (reload the page to see it)");
            } catch (IllegalArgumentException | IllegalStateException e) {
                AuditLog.record(request, "https certificate: " + action, null, AuditLog.FAILED, e.getMessage());
                request.setAttribute(ATTR_MESSAGE, "Not installed: " + e.getMessage());
            } catch (Exception e) {
                AdminMainProcessor.core.log.error("Unable to install the HTTPS certificate", e);
                AuditLog.record(request, "https certificate: " + action, null, AuditLog.FAILED, e.toString());
                request.setAttribute(ATTR_MESSAGE, "Not installed: " + e.getMessage());
            }
        }
        try {
            TlsCertificate.Info info = TlsCertificate.current();
            dom.setProperty("cert/subject", info.subject);
            dom.setProperty("cert/issuer", info.issuer);
            dom.setProperty("cert/names", info.names);
            dom.setProperty("cert/not_before", info.notBefore);
            dom.setProperty("cert/not_after", info.notAfter);
            dom.setProperty("cert/days_left", String.valueOf(info.daysLeft));
            dom.setProperty("cert/fingerprint", info.fingerprint);
            dom.setProperty("cert/key_type", info.keyType);
            dom.setProperty("cert/self_signed", String.valueOf(info.selfSigned));
        } catch (Exception e) {
            dom.setProperty("unavailable", e.getMessage() == null ? e.toString() : e.getMessage());
        }
        dom.setProperty("hostname", request.getServerName());
        return dom.getSource();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
