package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import hk.hku.cecid.edi.as2.AS2PlusProcessor;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDAO;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.ca.CertificateAuthority;

/**
 * CertificateHealthPageletAdaptor lists every certificate this gateway
 * relies on -- its own signing/decryption keys for AS2plus, ebMS and SFRM,
 * and every trading partner's encrypt/verify certificate across all three
 * protocols -- with expiry status, so an expiring certificate is caught
 * from one place instead of only being noticed when a partner's messages
 * start failing signature verification or encryption.
 *
 * Lives here (core admin) rather than under any one protocol's own admin
 * page for the same reason Listener Ports does: it is a cross-cutting view
 * over all three protocol plugins, not a feature of any single one.
 */
public class CertificateHealthPageletAdaptor extends AdminPageletAdaptor {

    private static final int EXPIRING_SOON_DAYS = 30;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/certificates", "");

        if ("post".equalsIgnoreCase(request.getMethod())
                && "generate_csr".equalsIgnoreCase(request.getParameter(REQ_PARAM_ACTION))) {
            handleGenerateCsr(request, dom);
        }

        int[] counts = new int[] { 0, 0, 0, 0 }; // total, valid, expiring_soon, expired
        int[] index = new int[] { 0 };

        appendGatewayCerts(dom, counts, index);
        appendAs2PlusPartnerCerts(dom, counts, index);
        appendEbmsPartnerCerts(dom, counts, index);
        appendSfrmPartnerCerts(dom, counts, index);

        dom.setProperty("summary/total_count", String.valueOf(counts[0]));
        dom.setProperty("summary/valid_count", String.valueOf(counts[1]));
        dom.setProperty("summary/expiring_soon_count", String.valueOf(counts[2]));
        dom.setProperty("summary/expired_count", String.valueOf(counts[3]));

        return dom.getSource();
    }

    /**
     * Generates a PKCS#10 Certificate Signing Request from one of the
     * gateway's own existing keystore key pairs (AS2plus/ebMS-signature/
     * ebMS-decryption/SFRM), signed with that same private key. The
     * resulting PEM is rendered back into the page for the admin to copy
     * and submit to an external CA -- the corresponding certificate, once
     * issued, replaces only the certificate entry in that same keystore
     * (the private key never leaves the keystore or gets regenerated here).
     */
    private void handleGenerateCsr(HttpServletRequest request, PropertyTree dom) {
        String keyId = request.getParameter("key_id");
        String commonName = trimOrNull(request.getParameter("csr_cn"));
        String org = trimOrNull(request.getParameter("csr_o"));
        String orgUnit = trimOrNull(request.getParameter("csr_ou"));
        String locality = trimOrNull(request.getParameter("csr_l"));
        String state = trimOrNull(request.getParameter("csr_st"));
        String country = trimOrNull(request.getParameter("csr_c"));
        boolean generateNewKey = request.getParameter("generate_new_key") != null;
        GeneralNames san = CertificateAuthority.parseSanEntries(request.getParameter("csr_san"));

        if (commonName == null) {
            request.setAttribute(ATTR_MESSAGE, "Common Name (CN) cannot be empty");
            return;
        }

        StringBuilder dn = new StringBuilder("CN=" + commonName);
        if (orgUnit != null) dn.append(",OU=").append(orgUnit);
        if (org != null) dn.append(",O=").append(org);
        if (locality != null) dn.append(",L=").append(locality);
        if (state != null) dn.append(",ST=").append(state);
        if (country != null) dn.append(",C=").append(country);

        try {
            PrivateKey privateKey;
            PublicKey publicKey;
            String generatedKeyPem = null;

            if (generateNewKey) {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048, new SecureRandom());
                KeyPair keyPair = keyGen.generateKeyPair();
                privateKey = keyPair.getPrivate();
                publicKey = keyPair.getPublic();

                StringWriter keyWriter = new StringWriter();
                PemWriter keyPemWriter = new PemWriter(keyWriter);
                keyPemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
                keyPemWriter.close();
                generatedKeyPem = keyWriter.toString();
            } else {
                KeyStoreManager ksm = keyStoreManagerFor(keyId);
                if (ksm == null) {
                    request.setAttribute(ATTR_MESSAGE, "Unknown key '" + keyId + "'");
                    return;
                }
                privateKey = ksm.getPrivateKey();
                publicKey = ksm.getPublicKey();
            }

            JcaPKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Name(dn.toString()), publicKey);
            if (san != null) {
                Extensions extensions = new Extensions(new Extension[] {
                        new Extension(Extension.subjectAlternativeName, false, san.getEncoded())
                });
                builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
            }
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
            PKCS10CertificationRequest csr = builder.build(signer);

            StringWriter sw = new StringWriter();
            PemWriter pemWriter = new PemWriter(sw);
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
            pemWriter.close();

            dom.setProperty("/certificates/generated_csr/key_id", generateNewKey ? "(newly generated key)" : keyId);
            dom.setProperty("/certificates/generated_csr/subject", dn.toString());
            dom.setProperty("/certificates/generated_csr/pem", sw.toString());
            if (generatedKeyPem != null) {
                dom.setProperty("/certificates/generated_csr/private_key_pem", generatedKeyPem);
            }
            request.setAttribute(ATTR_MESSAGE, generateNewKey
                    ? "CSR and private key generated -- copy BOTH below now. The private key is not stored "
                            + "anywhere and cannot be retrieved again."
                    : "CSR generated for '" + keyId
                            + "' -- copy it below and submit to your CA. This does not replace the "
                            + "current certificate; import the CA's response separately once issued.");
        } catch (OperatorCreationException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to generate CSR: " + e.getMessage());
        } catch (IOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to generate CSR: " + e.getMessage());
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to generate CSR: " + e.getMessage());
        }
    }

    private KeyStoreManager keyStoreManagerFor(String keyId) {
        if ("as2plus".equals(keyId)) {
            return AS2PlusProcessor.getInstance().getKeyStoreManager();
        } else if ("ebms-signature".equals(keyId)) {
            return EbmsProcessor.getKeyStoreManagerForSignature();
        } else if ("ebms-decryption".equals(keyId)) {
            return EbmsProcessor.getKeyStoreManagerForDecryption();
        } else if ("sfrm".equals(keyId)) {
            return SFRMProcessor.getInstance().getKeyStoreManager();
        }
        return null;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private void appendGatewayCerts(PropertyTree dom, int[] counts, int[] index) {
        try {
            KeyStoreManager as2plusKsm = AS2PlusProcessor.getInstance().getKeyStoreManager();
            appendCert(dom, counts, index, "Gateway Key", "AS2plus", "Signing / Decryption",
                    as2plusKsm == null ? null : as2plusKsm.getX509Certificate());
        } catch (Exception e) {
            appendError(dom, counts, index, "Gateway Key", "AS2plus", "Signing / Decryption", e);
        }

        try {
            KeyStoreManager ebmsSignKsm = EbmsProcessor.getKeyStoreManagerForSignature();
            appendCert(dom, counts, index, "Gateway Key", "ebMS", "Signing",
                    ebmsSignKsm == null ? null : ebmsSignKsm.getX509Certificate());
        } catch (Exception e) {
            appendError(dom, counts, index, "Gateway Key", "ebMS", "Signing", e);
        }

        try {
            KeyStoreManager ebmsDecryptKsm = EbmsProcessor.getKeyStoreManagerForDecryption();
            appendCert(dom, counts, index, "Gateway Key", "ebMS", "Decryption",
                    ebmsDecryptKsm == null ? null : ebmsDecryptKsm.getX509Certificate());
        } catch (Exception e) {
            appendError(dom, counts, index, "Gateway Key", "ebMS", "Decryption", e);
        }

        try {
            KeyStoreManager sfrmKsm = SFRMProcessor.getInstance().getKeyStoreManager();
            appendCert(dom, counts, index, "Gateway Key", "SFRM", "Signing / Decryption",
                    sfrmKsm == null ? null : sfrmKsm.getX509Certificate());
        } catch (Exception e) {
            appendError(dom, counts, index, "Gateway Key", "SFRM", "Signing / Decryption", e);
        }
    }

    /**
     * corvus-main-admin depends on both corvus-as2 (for its housecleaning
     * scheduler, see the hc.module package) and corvus-as2plus, and the two
     * plugins define hk.hku.cecid.edi.as2.dao.PartnershipDAO/DVO under the
     * identical fully-qualified name loaded by two different plugin
     * classloaders -- so a static cast against either one is a coin flip
     * that fails with a ClassCastException at runtime whenever it lands on
     * the other plugin's copy. Reflection sidesteps that: it dispatches by
     * method signature rather than requiring the caller and callee to agree
     * on which classloader's Class object "is" PartnershipDAO. The values
     * these methods return (String, X509Certificate) are ordinary JDK types
     * with no such ambiguity, so those are cast normally once retrieved.
     */
    private void appendAs2PlusPartnerCerts(PropertyTree dom, int[] counts, int[] index) {
        try {
            Object dao = AS2PlusProcessor.getInstance().getDAOFactory()
                    .createDAO("hk.hku.cecid.edi.as2.dao.PartnershipDAO");
            List partnerships = (List) dao.getClass().getMethod("findAllPartnerships").invoke(dao);
            for (Iterator it = partnerships.iterator(); it.hasNext(); ) {
                Object p = it.next();
                String as2From = (String) p.getClass().getMethod("getAS2From").invoke(p);
                String as2To = (String) p.getClass().getMethod("getAs2To").invoke(p);
                X509Certificate encryptCert =
                        (X509Certificate) p.getClass().getMethod("getEncryptX509Certificate").invoke(p);
                X509Certificate verifyCert =
                        (X509Certificate) p.getClass().getMethod("getVerifyX509Certificate").invoke(p);

                String owner = "AS2plus: " + as2From + " -> " + as2To;
                appendCert(dom, counts, index, "Partner Cert", owner, "Encrypt", encryptCert);
                appendCert(dom, counts, index, "Partner Cert", owner, "Verify", verifyCert);
            }
        } catch (Exception e) {
            appendError(dom, counts, index, "Partner Cert", "AS2plus", "(listing partnerships)", e);
        }
    }

    private void appendEbmsPartnerCerts(PropertyTree dom, int[] counts, int[] index) {
        try {
            hk.hku.cecid.ebms.spa.dao.PartnershipDAO dao = (hk.hku.cecid.ebms.spa.dao.PartnershipDAO)
                    EbmsProcessor.core.dao.createDAO(hk.hku.cecid.ebms.spa.dao.PartnershipDAO.class);
            List partnerships = dao.findAllPartnerships();
            for (Iterator it = partnerships.iterator(); it.hasNext(); ) {
                hk.hku.cecid.ebms.spa.dao.PartnershipDVO p = (hk.hku.cecid.ebms.spa.dao.PartnershipDVO) it.next();
                String owner = "ebMS: " + p.getCpaId() + " / " + p.getService() + " / " + p.getAction();
                appendCert(dom, counts, index, "Partner Cert", owner, "Encrypt", toX509(p.getEncryptCert()));
                appendCert(dom, counts, index, "Partner Cert", owner, "Sign", toX509(p.getSignCert()));
            }
        } catch (Exception e) {
            appendError(dom, counts, index, "Partner Cert", "ebMS", "(listing partnerships)", e);
        }
    }

    private void appendSfrmPartnerCerts(PropertyTree dom, int[] counts, int[] index) {
        try {
            SFRMPartnershipDAO dao = (SFRMPartnershipDAO) SFRMProcessor.getInstance().getDAOFactory().createDAO(SFRMPartnershipDAO.class);
            List partnerships = dao.findAllPartnerships();
            for (Iterator it = partnerships.iterator(); it.hasNext(); ) {
                SFRMPartnershipDVO p = (SFRMPartnershipDVO) it.next();
                String owner = "SFRM: " + p.getPartnershipId();
                X509Certificate encryptCert = null;
                X509Certificate verifyCert = null;
                try {
                    encryptCert = p.getEncryptX509Certificate();
                } catch (Exception ignore) {
                }
                try {
                    verifyCert = p.getVerifyX509Certificate();
                } catch (Exception ignore) {
                }
                appendCert(dom, counts, index, "Partner Cert", owner, "Encrypt", encryptCert);
                appendCert(dom, counts, index, "Partner Cert", owner, "Verify", verifyCert);
            }
        } catch (Exception e) {
            appendError(dom, counts, index, "Partner Cert", "SFRM", "(listing partnerships)", e);
        }
    }

    private X509Certificate toX509(byte[] der) {
        if (der == null || der.length == 0) {
            return null;
        }
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception e) {
            return null;
        }
    }

    private void appendCert(PropertyTree dom, int[] counts, int[] index,
            String category, String owner, String role, X509Certificate cert) {
        if (cert == null) {
            return;
        }

        index[0]++;
        String prefix = "cert[" + index[0] + "]";

        Date notAfter = cert.getNotAfter();
        long daysRemaining = (notAfter.getTime() - System.currentTimeMillis()) / (24L * 60 * 60 * 1000);

        String status;
        if (daysRemaining < 0) {
            status = "expired";
        } else if (daysRemaining <= EXPIRING_SOON_DAYS) {
            status = "expiring_soon";
        } else {
            status = "valid";
        }

        counts[0]++;
        if ("expired".equals(status)) {
            counts[3]++;
        } else if ("expiring_soon".equals(status)) {
            counts[2]++;
        } else {
            counts[1]++;
        }

        dom.setProperty(prefix + "/category", category);
        dom.setProperty(prefix + "/owner", owner);
        dom.setProperty(prefix + "/role", role);
        dom.setProperty(prefix + "/subject", cert.getSubjectDN().getName());
        dom.setProperty(prefix + "/issuer", cert.getIssuerDN().getName());
        dom.setProperty(prefix + "/not_after", DATE_FORMAT.format(notAfter));
        dom.setProperty(prefix + "/days_remaining", String.valueOf(daysRemaining));
        dom.setProperty(prefix + "/status", status);
    }

    private void appendError(PropertyTree dom, int[] counts, int[] index,
            String category, String owner, String role, Exception e) {
        index[0]++;
        String prefix = "cert[" + index[0] + "]";
        counts[0]++;
        counts[3]++;
        dom.setProperty(prefix + "/category", category);
        dom.setProperty(prefix + "/owner", owner);
        dom.setProperty(prefix + "/role", role);
        dom.setProperty(prefix + "/subject", "");
        dom.setProperty(prefix + "/issuer", "");
        dom.setProperty(prefix + "/not_after", "");
        dom.setProperty(prefix + "/days_remaining", "");
        dom.setProperty(prefix + "/status", "error");
        dom.setProperty(prefix + "/error", e.getMessage() == null ? e.toString() : e.getMessage());
    }
}
