package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.management.ObjectName;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

/**
 * The HTTPS certificate of the console and gateway: the PKCS#12 key store
 * hermes-tls.sh keeps on the repository volume (system properties
 * hermes.tls.keystore / hermes.tls.passwordFile). Replacing it writes a
 * new key store (the previous one kept as server.p12.previous) and has
 * Tomcat reload it, without a restart.
 */
public final class TlsCertificate {

    static final String ALIAS = "hermes";

    private TlsCertificate() {
    }

    /** What the Main > HTTPS Certificate page shows. */
    public static class Info {
        public String subject, issuer, names, notBefore, notAfter, fingerprint, keyType;
        public boolean selfSigned;
        public long daysLeft;
    }

    static File keystoreFile() {
        String path = System.getProperty("hermes.tls.keystore");
        if (path == null) {
            throw new IllegalStateException("this server has no managed HTTPS certificate"
                    + " (hermes.tls.keystore is not set)");
        }
        return new File(path);
    }

    static char[] password() throws Exception {
        String file = System.getProperty("hermes.tls.passwordFile");
        return new String(Files.readAllBytes(new File(file).toPath()), StandardCharsets.UTF_8).trim().toCharArray();
    }

    public static Info current() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(keystoreFile())) {
            ks.load(in, password());
        }
        return describe((X509Certificate) ks.getCertificate(ALIAS));
    }

    static Info describe(X509Certificate cert) throws Exception {
        Info info = new Info();
        info.subject = cert.getSubjectX500Principal().getName();
        info.issuer = cert.getIssuerX500Principal().getName();
        info.selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        List<String> names = new ArrayList<String>();
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();
        if (sans != null) {
            for (List<?> san : sans) {
                names.add(String.valueOf(san.get(1)));
            }
        }
        info.names = String.join(", ", names);
        info.notBefore = cert.getNotBefore().toString();
        info.notAfter = cert.getNotAfter().toString();
        info.daysLeft = (cert.getNotAfter().getTime() - System.currentTimeMillis()) / (24L * 3600 * 1000);
        info.keyType = cert.getPublicKey().getAlgorithm();
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        StringBuilder fp = new StringBuilder();
        for (byte b : digest) {
            fp.append(fp.length() == 0 ? "" : ":").append(String.format("%02X", b));
        }
        info.fingerprint = fp.toString();
        return info;
    }

    /**
     * Installs a certificate (with its chain) and private key given as PEM.
     *
     * @param keyPassword the private key's password if it is encrypted, else null
     */
    public static Info installPem(String certificatesPem, String keyPem, String keyPassword) throws Exception {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (PEMParser parser = new PEMParser(new StringReader(certificatesPem))) {
            for (Object o; (o = parser.readObject()) != null;) {
                if (o instanceof X509CertificateHolder) {
                    certs.add((X509Certificate) cf.generateCertificate(
                            new ByteArrayInputStream(((X509CertificateHolder) o).getEncoded())));
                }
            }
        }
        if (certs.isEmpty()) {
            throw new IllegalArgumentException("no certificate found in the certificate PEM");
        }
        return install(readKey(keyPem, keyPassword), certs);
    }

    static PrivateKey readKey(String pem, String password) throws Exception {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            for (Object o; (o = parser.readObject()) != null;) {
                if (o instanceof PEMKeyPair) {
                    return converter.getKeyPair((PEMKeyPair) o).getPrivate();
                }
                if (o instanceof PrivateKeyInfo) {
                    return converter.getPrivateKey((PrivateKeyInfo) o);
                }
                if (o instanceof PKCS8EncryptedPrivateKeyInfo || o instanceof PEMEncryptedKeyPair) {
                    if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("the private key is encrypted: enter its password");
                    }
                    try {
                        if (o instanceof PKCS8EncryptedPrivateKeyInfo) {
                            return converter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) o).decryptPrivateKeyInfo(
                                    new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray())));
                        }
                        return converter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(
                                new JcePEMDecryptorProviderBuilder().build(password.toCharArray()))).getPrivate();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("the private key password is wrong");
                    }
                }
            }
        }
        throw new IllegalArgumentException("no private key found in the key PEM");
    }

    /** Installs the first key (and its chain) of a PKCS#12 / PFX file. */
    public static Info installPkcs12(byte[] file, String password) throws Exception {
        KeyStore source = KeyStore.getInstance("PKCS12");
        char[] pw = password == null ? new char[0] : password.toCharArray();
        try {
            source.load(new ByteArrayInputStream(file), pw);
        } catch (Exception e) {
            throw new IllegalArgumentException("not a PKCS#12 file, or the password is wrong");
        }
        for (Enumeration<String> aliases = source.aliases(); aliases.hasMoreElements();) {
            String alias = aliases.nextElement();
            if (source.isKeyEntry(alias)) {
                List<X509Certificate> chain = new ArrayList<X509Certificate>();
                for (Certificate c : source.getCertificateChain(alias)) {
                    chain.add((X509Certificate) c);
                }
                return install((PrivateKey) source.getKey(alias, pw), chain);
            }
        }
        throw new IllegalArgumentException("the PKCS#12 file holds no private key");
    }

    /** Replaces the certificate with a new self-signed one for hostname. */
    public static Info generateSelfSigned(String hostname) throws Exception {
        if (hostname == null || !hostname.matches("[A-Za-z0-9.-]{1,253}")) {
            throw new IllegalArgumentException("enter a host name such as gateway.example.com");
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X500Name name = new X500Name("CN=" + hostname + ", OU=Hermes, O=Hermes2");
        Date from = new Date(System.currentTimeMillis() - 3600 * 1000);
        Date to = new Date(System.currentTimeMillis() + 825L * 24 * 3600 * 1000);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                new BigInteger(64, new SecureRandom()), from, to, name, kp.getPublic());
        List<GeneralName> sans = new ArrayList<GeneralName>();
        sans.add(new GeneralName(GeneralName.dNSName, hostname));
        if (!"localhost".equals(hostname)) {
            sans.add(new GeneralName(GeneralName.dNSName, "localhost"));
        }
        sans.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(sans.toArray(new GeneralName[0])));
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate())));
        List<X509Certificate> chain = new ArrayList<X509Certificate>();
        chain.add(cert);
        return install(kp.getPrivate(), chain);
    }

    /**
     * Checks the key and certificates, writes them as the key store and has
     * Tomcat reload it.
     */
    static Info install(PrivateKey key, List<X509Certificate> certs) throws Exception {
        X509Certificate leaf = null;
        for (X509Certificate c : certs) {
            if (matches(key, c)) {
                leaf = c;
                break;
            }
        }
        if (leaf == null) {
            throw new IllegalArgumentException("the private key does not belong to the certificate");
        }
        try {
            leaf.checkValidity();
        } catch (Exception e) {
            throw new IllegalArgumentException("the certificate is expired or not yet valid ("
                    + leaf.getNotBefore() + " to " + leaf.getNotAfter() + ")");
        }
        List<Certificate> chain = new ArrayList<Certificate>();
        chain.add(leaf);
        for (X509Certificate c : certs) {
            if (c != leaf) {
                chain.add(c);
            }
        }

        char[] password = password();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, key, password, chain.toArray(new Certificate[0]));
        File target = keystoreFile();
        File temp = new File(target.getPath() + ".new");
        try (OutputStream out = new FileOutputStream(temp)) {
            ks.store(out, password);
        }
        temp.setReadable(false, false);
        temp.setReadable(true, true);
        if (target.exists()) {
            Files.copy(target.toPath(), new File(target.getPath() + ".previous").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        reload();
        return describe(leaf);
    }

    /** Whether the private key is the one of the certificate's public key. */
    static boolean matches(PrivateKey key, X509Certificate cert) {
        try {
            String algorithm = "EC".equals(key.getAlgorithm()) ? "SHA256withECDSA"
                    : "RSA".equals(key.getAlgorithm()) ? "SHA256withRSA" : null;
            if (algorithm == null || !key.getAlgorithm().equals(cert.getPublicKey().getAlgorithm())) {
                return false;
            }
            byte[] data = new byte[32];
            new SecureRandom().nextBytes(data);
            Signature sign = Signature.getInstance(algorithm);
            sign.initSign(key);
            sign.update(data);
            byte[] signature = sign.sign();
            Signature verify = Signature.getInstance(algorithm);
            verify.initVerify(cert.getPublicKey());
            verify.update(data);
            return verify.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /** Has Tomcat's HTTPS connector re-read the key store. */
    static void reload() throws Exception {
        String port = System.getProperty("catalina.https.port", "8443");
        ManagementFactory.getPlatformMBeanServer().invoke(
                new ObjectName("Catalina:type=ProtocolHandler,port=" + port), "reloadSslHostConfigs",
                new Object[0], new String[0]);
    }
}
