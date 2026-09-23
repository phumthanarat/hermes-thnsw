package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.ca.CertificateAuthority;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDVO;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

/**
 * CertificateAuthorityPageletAdaptor lets an administrator issue
 * certificates to trading partners directly from a pasted CSR, using this
 * gateway's own internal CA, and revoke ones that were issued. Revoking a
 * certificate here takes effect the next time it's checked, since issued
 * certificates carry a CRL Distribution Point pointing at this same
 * gateway's own CaCrlListener endpoint.
 */
public class CertificateAuthorityPageletAdaptor extends AdminPageletAdaptor {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/ca", "");

        try {
            X509Certificate caCert = CertificateAuthority.getCertificate();
            dom.setProperty("ca_info/subject", caCert.getSubjectX500Principal().getName());
            dom.setProperty("ca_info/not_after", DATE_FORMAT.format(caCert.getNotAfter()));
            dom.setProperty("ca_info/crl_url", crlUrl(request));
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to load the internal CA: " + e.getMessage());
            return dom.getSource();
        }

        String action = request.getParameter(REQ_PARAM_ACTION);
        if ("post".equalsIgnoreCase(request.getMethod())) {
            if ("issue_cert".equalsIgnoreCase(action)) {
                handleIssueCert(request, dom);
            } else if ("quick_issue".equalsIgnoreCase(action)) {
                handleQuickIssue(request, dom);
            } else if ("revoke_cert".equalsIgnoreCase(action)) {
                handleRevokeCert(request);
            }
        }

        appendIssuedCerts(dom);

        return dom.getSource();
    }

    private String crlUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + "/corvus/httpd/ca/crl";
    }

    private void handleIssueCert(HttpServletRequest request, PropertyTree dom) {
        String csrPem = request.getParameter("csr_pem");
        int validityDays = parseValidityDays(request.getParameter("validity_days"));
        GeneralNames san = CertificateAuthority.parseSanEntries(request.getParameter("cert_san"));

        if (csrPem == null || csrPem.trim().length() == 0) {
            request.setAttribute(ATTR_MESSAGE, "Paste a CSR (PEM) to issue a certificate");
            return;
        }

        try {
            PEMParser pemParser = new PEMParser(new StringReader(csrPem));
            Object parsed = pemParser.readObject();
            pemParser.close();

            if (!(parsed instanceof PKCS10CertificationRequest)) {
                request.setAttribute(ATTR_MESSAGE, "That doesn't look like a CSR (PKCS#10 CERTIFICATE REQUEST)");
                return;
            }

            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) parsed;
            X509Certificate issued = CertificateAuthority.issueCertificate(csr, validityDays, crlUrl(request), san);

            renderIssuedCert(dom, issued, null);
            request.setAttribute(ATTR_MESSAGE, "Certificate issued -- copy it below and hand it to the partner. "
                    + "Set it as their encrypt/verify certificate on the relevant partnership.");
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to issue certificate: " + e.getMessage());
        }
    }

    /**
     * Issues a certificate for a brand new key pair the CA generates itself,
     * skipping the CSR step entirely -- for when this gateway should
     * originate the partner's identity rather than the partner supplying
     * their own CSR. The private key is shown once, here, and never stored;
     * losing it means starting over with a new certificate.
     */
    private void handleQuickIssue(HttpServletRequest request, PropertyTree dom) {
        String subjectDn = request.getParameter("quick_subject_dn");
        int validityDays = parseValidityDays(request.getParameter("quick_validity_days"));
        GeneralNames san = CertificateAuthority.parseSanEntries(request.getParameter("quick_san"));

        if (subjectDn == null || subjectDn.trim().length() == 0) {
            request.setAttribute(ATTR_MESSAGE, "Subject DN cannot be empty, e.g. CN=partner.example.com,O=Partner Co");
            return;
        }

        try {
            CertificateAuthority.KeyAndCert result =
                    CertificateAuthority.quickIssue(subjectDn.trim(), validityDays, crlUrl(request), san);

            StringWriter keyWriter = new StringWriter();
            PemWriter keyPemWriter = new PemWriter(keyWriter);
            keyPemWriter.writeObject(new PemObject("PRIVATE KEY", result.privateKey.getEncoded()));
            keyPemWriter.close();

            renderIssuedCert(dom, result.certificate, keyWriter.toString());
            request.setAttribute(ATTR_MESSAGE, "Certificate and private key generated -- copy BOTH below now. "
                    + "The private key is not stored anywhere and cannot be retrieved again.");
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to issue certificate: " + e.getMessage());
        }
    }

    private void renderIssuedCert(PropertyTree dom, X509Certificate issued, String privateKeyPem) throws Exception {
        StringWriter sw = new StringWriter();
        PemWriter pemWriter = new PemWriter(sw);
        pemWriter.writeObject(new PemObject("CERTIFICATE", issued.getEncoded()));
        pemWriter.close();

        dom.setProperty("issued_cert/subject", issued.getSubjectX500Principal().getName());
        dom.setProperty("issued_cert/serial_number", issued.getSerialNumber().toString());
        dom.setProperty("issued_cert/pem", sw.toString());
        if (privateKeyPem != null) {
            dom.setProperty("issued_cert/private_key_pem", privateKeyPem);
        }
    }

    private int parseValidityDays(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void handleRevokeCert(HttpServletRequest request) {
        String serialNumber = request.getParameter("serial_number");
        try {
            CaIssuedCertDAO dao = (CaIssuedCertDAO) AdminMainProcessor.core.dao.createDAO(CaIssuedCertDAO.class);
            CaIssuedCertDVO dvo = dao.findBySerialNumber(serialNumber);
            if (dvo != null && !dvo.isRevoked()) {
                dvo.setRevoked(true);
                dvo.setRevokedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE, "Certificate " + serialNumber + " revoked");
            }
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to revoke certificate: " + e.getMessage());
        }
    }

    private void appendIssuedCerts(PropertyTree dom) {
        try {
            CaIssuedCertDAO dao = (CaIssuedCertDAO) AdminMainProcessor.core.dao.createDAO(CaIssuedCertDAO.class);
            List certs = dao.findAllCerts();
            int i = 0;
            for (Iterator it = certs.iterator(); it.hasNext(); ) {
                i++;
                CaIssuedCertDVO dvo = (CaIssuedCertDVO) it.next();
                String prefix = "issued_certs/cert[" + i + "]";
                dom.setProperty(prefix + "/serial_number", dvo.getSerialNumber());
                dom.setProperty(prefix + "/subject_dn", dvo.getSubjectDn());
                dom.setProperty(prefix + "/revoked", String.valueOf(dvo.isRevoked()));
                dom.setProperty(prefix + "/issued_timestamp",
                        dvo.getIssuedTimestamp() == null ? "" : DATE_FORMAT.format(dvo.getIssuedTimestamp()));
            }
        } catch (Exception e) {
            // Leave the list empty; the page's own ATTR_MESSAGE (if any) already explains why.
        }
    }
}
