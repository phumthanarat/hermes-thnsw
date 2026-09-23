package hk.hku.cecid.piazza.corvus.core.main.admin.ca;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDVO;

/**
 * CertificateAuthority is a self-signed root CA this gateway can use to
 * issue certificates directly to trading partners, instead of every
 * partnership needing a certificate from an external CA. Its root key pair
 * is generated once (RSA 2048, 10-year validity) on first use and persisted
 * to a PKCS12 file alongside this plugin's other configuration --
 * see the caveat on that in {@link #resolveKeystoreFile()}.
 *
 * Certificates it issues carry a CRL Distribution Point pointing back at
 * this gateway's own CRL endpoint ({@code CaCrlListener}, served under
 * /corvus/httpd/ca/crl), so {@link
 * hk.hku.cecid.piazza.commons.security.RevocationChecker} can actually
 * validate them end to end: revoking a partner's certificate here takes
 * effect the next time that partner's signature is checked, without
 * needing any out-of-band notification.
 */
public class CertificateAuthority {

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String ALIAS = "ca";
    private static final char[] STORE_PASS = "corvus-internal-ca".toCharArray();
    private static final String CA_SUBJECT_DN = "CN=Hermes Gateway Internal CA,O=Hermes2";
    private static final int CA_VALIDITY_DAYS = 3650;
    private static final int ISSUED_CERT_VALIDITY_DAYS_DEFAULT = 730;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static KeyStore keyStore;

    private CertificateAuthority() {
    }

    /**
     * Resolves the file the CA's own keystore is persisted to: a sibling of
     * admin.dao.xml inside this plugin's deployed conf directory.
     *
     * <p><b>Caveat:</b> that directory is part of the plugin's deployed
     * files, not one of this image's declared Docker volumes (only
     * hermes_home/logs and hermes_home/repository are), so a fresh image
     * rebuild/redeploy loses the CA and every certificate it issued stops
     * validating against it. Mount the plugin directory as a volume (or
     * back this keystore up externally) before relying on this in
     * production.
     */
    private static File resolveKeystoreFile() throws Exception {
        URL adminDaoXml = AdminMainProcessor.getModuleGroup().getSystemModule().getClassLoader()
                .getResource("hk/hku/cecid/piazza/corvus/core/main/admin/conf/admin.dao.xml");
        File confDir = new File(adminDaoXml.toURI()).getParentFile();
        return new File(confDir, "ca.p12");
    }

    private static synchronized KeyStore getKeyStore() throws Exception {
        if (keyStore != null) {
            return keyStore;
        }

        File file = resolveKeystoreFile();
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, "BC");

        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            try {
                ks.load(in, STORE_PASS);
            } finally {
                in.close();
            }
        } else {
            ks.load(null, STORE_PASS);
            generateRootCa(ks);
            FileOutputStream out = new FileOutputStream(file);
            try {
                ks.store(out, STORE_PASS);
            } finally {
                out.close();
            }
        }

        keyStore = ks;
        publishCertForRevocationChecker(ks);
        return keyStore;
    }

    /**
     * Publishes this CA's own certificate to the well-known shared path
     * {@link hk.hku.cecid.piazza.commons.security.RevocationChecker} reads,
     * so it can validate certs this CA issued without corvus-main-admin
     * needing to be a compile-time dependency of every protocol plugin.
     */
    private static void publishCertForRevocationChecker(KeyStore ks) {
        try {
            X509Certificate cert = (X509Certificate) ks.getCertificate(ALIAS);
            File out = new File("/hermes_home/ca-cert.pem");
            java.io.FileWriter writer = new java.io.FileWriter(out);
            org.bouncycastle.util.io.pem.PemWriter pemWriter = new org.bouncycastle.util.io.pem.PemWriter(writer);
            pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject("CERTIFICATE", cert.getEncoded()));
            pemWriter.close();
        } catch (Exception e) {
            // Best-effort -- if hermes_home isn't writable/mounted here for
            // some reason, RevocationChecker just treats CA-issued certs as
            // not checkable, same as any other external CA it doesn't hold.
        }
    }

    private static void generateRootCa(KeyStore ks) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + CA_VALIDITY_DAYS * 24L * 60 * 60 * 1000);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        X500Name subject = new X500Name(CA_SUBJECT_DN);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        ks.setKeyEntry(ALIAS, keyPair.getPrivate(), STORE_PASS, new Certificate[] { cert });
    }

    public static X509Certificate getCertificate() throws Exception {
        return (X509Certificate) getKeyStore().getCertificate(ALIAS);
    }

    private static PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) getKeyStore().getKey(ALIAS, STORE_PASS);
    }

    /**
     * Signs a CSR into an end-entity certificate, records it in
     * ca_issued_cert, and returns it.
     *
     * @param csr the applicant's certificate signing request.
     * @param validityDays how long the issued certificate should be valid for
     *        ({@link #ISSUED_CERT_VALIDITY_DAYS_DEFAULT} if not positive).
     * @param crlUrl the URL this certificate's CRL Distribution Point should
     *        point at (this gateway's own CaCrlListener endpoint).
     * @param sanEntries Subject Alternative Names, e.g. {"DNS:partner.example.com",
     *        "IP:203.0.113.10"} ({@link #parseSanEntries(String)}); no SAN
     *        extension is added if null/empty.
     */
    public static X509Certificate issueCertificate(PKCS10CertificationRequest csr, int validityDays, String crlUrl,
            GeneralNames sanEntries) throws Exception {

        X509Certificate caCert = getCertificate();
        PrivateKey caKey = getPrivateKey();

        int days = validityDays > 0 ? validityDays : ISSUED_CERT_VALIDITY_DAYS_DEFAULT;
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + days * 24L * 60 * 60 * 1000);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                new X500Name(caCert.getSubjectX500Principal().getName()),
                serial, notBefore, notAfter, csr.getSubject(), csr.getSubjectPublicKeyInfo());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        addCommonExtensions(builder, extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()),
                extUtils, caCert, crlUrl, sanEntries);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caKey);
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate issued = new JcaX509CertificateConverter().getCertificate(holder);

        recordIssuedCert(issued, serial);
        return issued;
    }

    /**
     * Generates a fresh RSA-2048 key pair and issues it a certificate
     * directly, without a separate CSR round-trip -- a shortcut for issuing
     * a partner an identity the CA itself originates end to end. The private
     * key is returned to the caller and not persisted anywhere; it is the
     * caller's responsibility to hand it to whoever needs it and not lose it.
     */
    public static KeyAndCert quickIssue(String subjectDn, int validityDays, String crlUrl, GeneralNames sanEntries)
            throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        X509Certificate caCert = getCertificate();
        PrivateKey caKey = getPrivateKey();

        int days = validityDays > 0 ? validityDays : ISSUED_CERT_VALIDITY_DAYS_DEFAULT;
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + days * 24L * 60 * 60 * 1000);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name(caCert.getSubjectX500Principal().getName()),
                serial, notBefore, notAfter, new X500Name(subjectDn), keyPair.getPublic());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        addCommonExtensions(builder, extUtils.createSubjectKeyIdentifier(keyPair.getPublic()),
                extUtils, caCert, crlUrl, sanEntries);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caKey);
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate issued = new JcaX509CertificateConverter().getCertificate(holder);

        recordIssuedCert(issued, serial);
        return new KeyAndCert(keyPair.getPrivate(), issued);
    }

    /**
     * Adds the extensions common to every certificate this CA issues
     * (BasicConstraints, KeyUsage, SKI, AKI, CRLDP, and optionally SAN) to
     * {@code builder}. {@link JcaX509v3CertificateBuilder} (used by {@link
     * #quickIssue}) is itself an {@link X509v3CertificateBuilder}, so both
     * callers share this without needing their own copy.
     */
    private static void addCommonExtensions(X509v3CertificateBuilder builder, SubjectKeyIdentifier subjectKeyId,
            JcaX509ExtensionUtils extUtils, X509Certificate caCert, String crlUrl, GeneralNames sanEntries)
            throws Exception {

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.nonRepudiation
                        | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment));
        builder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyId);
        builder.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCert.getPublicKey()));

        if (crlUrl != null && crlUrl.length() > 0) {
            GeneralName crlName = new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl);
            DistributionPointName dpName = new DistributionPointName(new GeneralNames(crlName));
            CRLDistPoint crlDistPoint = new CRLDistPoint(
                    new DistributionPoint[] { new DistributionPoint(dpName, null, null) });
            builder.addExtension(Extension.cRLDistributionPoints, false, crlDistPoint);
        }

        if (sanEntries != null && sanEntries.getNames().length > 0) {
            builder.addExtension(Extension.subjectAlternativeName, false, sanEntries);
        }
    }

    private static void recordIssuedCert(X509Certificate issued, BigInteger serial) throws Exception {
        CaIssuedCertDAO dao = (CaIssuedCertDAO) AdminMainProcessor.core.dao.createDAO(CaIssuedCertDAO.class);
        CaIssuedCertDVO dvo = (CaIssuedCertDVO) dao.createDVO();
        dvo.setSerialNumber(serial.toString());
        dvo.setSubjectDn(issued.getSubjectX500Principal().getName());
        dvo.setCert(issued.getEncoded());
        dvo.setRevoked(false);
        dao.create(dvo);
    }

    /**
     * Parses a comma-separated SAN list like
     * {@code "DNS:partner.example.com, IP:203.0.113.10, EMAIL:ops@partner.example.com"}
     * into a {@link GeneralNames}. Unknown/malformed entries are skipped.
     * Returns null (no SAN extension) if the input is blank.
     */
    public static GeneralNames parseSanEntries(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return null;
        }
        java.util.List names = new java.util.ArrayList();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String entry = parts[i].trim();
            if (entry.length() == 0) {
                continue;
            }
            int colon = entry.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String type = entry.substring(0, colon).trim().toUpperCase();
            String value = entry.substring(colon + 1).trim();
            if (value.length() == 0) {
                continue;
            }
            int tag;
            if ("DNS".equals(type)) {
                tag = GeneralName.dNSName;
            } else if ("IP".equals(type)) {
                tag = GeneralName.iPAddress;
            } else if ("EMAIL".equals(type)) {
                tag = GeneralName.rfc822Name;
            } else if ("URI".equals(type)) {
                tag = GeneralName.uniformResourceIdentifier;
            } else {
                continue;
            }
            names.add(new GeneralName(tag, value));
        }
        if (names.isEmpty()) {
            return null;
        }
        return new GeneralNames((GeneralName[]) names.toArray(new GeneralName[names.size()]));
    }

    /** A freshly generated private key together with the certificate the CA issued for it. */
    public static class KeyAndCert {
        public final PrivateKey privateKey;
        public final X509Certificate certificate;

        public KeyAndCert(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }
    }

    /**
     * Builds a DER-encoded CRL listing every revoked certificate this CA has
     * issued. Called by CaCrlListener on every fetch -- cheap enough (a
     * handful of DB rows, one signature) not to need caching.
     */
    public static byte[] buildCrl() throws Exception {
        X509Certificate caCert = getCertificate();
        PrivateKey caKey = getPrivateKey();

        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + 24L * 60 * 60 * 1000);
        X509v2CRLBuilder builder = new X509v2CRLBuilder(
                new X500Name(caCert.getSubjectX500Principal().getName()), now);
        builder.setNextUpdate(nextUpdate);

        CaIssuedCertDAO dao = (CaIssuedCertDAO) AdminMainProcessor.core.dao.createDAO(CaIssuedCertDAO.class);
        java.util.List revoked = dao.findRevokedCerts();
        for (java.util.Iterator it = revoked.iterator(); it.hasNext(); ) {
            CaIssuedCertDVO dvo = (CaIssuedCertDVO) it.next();
            Date revokedAt = dvo.getRevokedTimestamp() == null ? now : dvo.getRevokedTimestamp();
            builder.addCRLEntry(new BigInteger(dvo.getSerialNumber()), revokedAt, 0);
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caKey);
        return new JcaX509CRLConverter().getCRL(builder.build(signer)).getEncoded();
    }
}
