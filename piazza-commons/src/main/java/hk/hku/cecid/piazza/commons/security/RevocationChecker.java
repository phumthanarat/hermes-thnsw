package hk.hku.cecid.piazza.commons.security;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * RevocationChecker checks whether an X509Certificate has been revoked, via
 * OCSP (preferred) falling back to CRL, using the JDK's own PKIX revocation
 * machinery ({@link PKIXRevocationChecker}) rather than a hand-rolled OCSP/
 * CRL client.
 *
 * <p>PKIX revocation checking is defined in terms of a certificate path
 * anchored at a trusted issuer certificate, but this gateway only stores a
 * single leaf certificate per partnership (no issuer chain) -- so a
 * meaningful check is only possible when a trust anchor for that leaf can be
 * established, which is the case in exactly two situations:
 * <ul>
 *   <li>the certificate is self-signed (its own issuer and subject match),
 *       which covers this gateway's own certificates and many small trading
 *       partners' self-signed certs -- the cert is used as its own anchor;
 *   <li>an explicit issuer certificate is supplied (e.g. this gateway's own
 *       internal CA, see corvus-main-admin's CertificateAuthority) -- the
 *       issuer is used as the anchor for a two-certificate path.
 * </ul>
 * For any other certificate (issued by an external CA whose certificate this
 * gateway does not hold), there is no trust anchor to validate against, so
 * {@link #checkRevocation(X509Certificate, X509Certificate)} returns {@link
 * Result#NOT_CHECKABLE} rather than guessing.
 *
 * <p>Whenever the check genuinely cannot be completed -- no CRL/OCSP
 * distribution point on the certificate, the responder or CRL endpoint is
 * unreachable, etc -- this fails <b>open</b> ({@link Result#UNKNOWN}) rather
 * than rejecting the message, since an unreachable revocation service is far
 * more likely than an actual compromise and a signature-verified partner
 * should not be locked out by that. Only a definitive REVOKED response
 * results in {@link Result#REVOKED}.
 */
public class RevocationChecker {

    public enum Result {
        /** Confirmed not revoked by a reachable OCSP responder or CRL. */
        NOT_REVOKED,
        /** Confirmed revoked -- the caller should reject the message. */
        REVOKED,
        /** No trust anchor available for this certificate; not attempted. */
        NOT_CHECKABLE,
        /** A trust anchor was available but the check could not be completed
         *  (no revocation info on the cert, responder/CRL unreachable, etc). */
        UNKNOWN
    }

    /**
     * Well-known location this gateway's internal CA (see corvus-main-admin's
     * CertificateAuthority) publishes its own certificate to, so that any
     * module -- without a compile-time dependency on corvus-main-admin --
     * can use it as a trust anchor for certs it issued. A plain shared file
     * under hermes_home rather than a Java API, since piazza-commons sits
     * below every protocol plugin and corvus-main-admin sits above all of
     * them; a direct reference either way would invert that layering.
     */
    private static final String INTERNAL_CA_CERT_PATH = "/hermes_home/ca-cert.pem";

    /**
     * Directory an admin's imported external CA root certificates (see
     * corvus-main-admin's Certificate Authority page, "Trusted External
     * CAs") are published to, one .pem file each -- same rationale as
     * {@link #INTERNAL_CA_CERT_PATH}: a shared file location rather than a
     * Java API, to avoid a layering inversion.
     */
    private static final String TRUSTED_CAS_DIR = "/hermes_home/trusted-cas";

    private RevocationChecker() {
    }

    /**
     * Checks revocation of {@code cert} using whatever trust anchor can be
     * established for it: itself, if self-signed; this gateway's own
     * internal CA, if that CA issued it; or an admin-imported external CA
     * root certificate whose subject matches the issuer. Returns {@link
     * Result#NOT_CHECKABLE} if none of those apply, since no anchor is
     * available to validate the chain against.
     */
    public static Result checkRevocation(X509Certificate cert) {
        if (cert == null) {
            return Result.NOT_CHECKABLE;
        }
        if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
            return checkRevocation(cert, cert);
        }

        X509Certificate internalCa = loadCertFile(new File(INTERNAL_CA_CERT_PATH));
        if (internalCa != null && cert.getIssuerX500Principal().equals(internalCa.getSubjectX500Principal())) {
            return checkRevocation(cert, internalCa);
        }

        List trustedCas = loadTrustedCaCerts();
        for (int i = 0; i < trustedCas.size(); i++) {
            X509Certificate trustedCa = (X509Certificate) trustedCas.get(i);
            if (cert.getIssuerX500Principal().equals(trustedCa.getSubjectX500Principal())) {
                return checkRevocation(cert, trustedCa);
            }
        }

        return Result.NOT_CHECKABLE;
    }

    private static List loadTrustedCaCerts() {
        List certs = new ArrayList();
        File dir = new File(TRUSTED_CAS_DIR);
        File[] files = dir.listFiles();
        if (files == null) {
            return certs;
        }
        for (int i = 0; i < files.length; i++) {
            X509Certificate cert = loadCertFile(files[i]);
            if (cert != null) {
                certs.add(cert);
            }
        }
        return certs;
    }

    private static X509Certificate loadCertFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Checks revocation of {@code cert}, anchored at {@code issuer} if given
     * (or at {@code cert} itself if {@code issuer} is null and {@code cert}
     * is self-signed).
     */
    public static Result checkRevocation(X509Certificate cert, X509Certificate issuer) {
        if (cert == null) {
            return Result.NOT_CHECKABLE;
        }

        X509Certificate anchorCert = issuer;
        if (anchorCert == null) {
            boolean selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
            if (!selfSigned) {
                return Result.NOT_CHECKABLE;
            }
            anchorCert = cert;
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(Collections.singletonList(cert));
            TrustAnchor anchor = new TrustAnchor(anchorCert, null);

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXRevocationChecker revocationChecker =
                    (PKIXRevocationChecker) validator.getRevocationChecker();
            revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL));

            PKIXParameters params = new PKIXParameters(Collections.singleton(anchor));
            params.addCertPathChecker(revocationChecker);
            params.setRevocationEnabled(false);

            validator.validate(certPath, params);

            if (!revocationChecker.getSoftFailExceptions().isEmpty()) {
                // SOFT_FAIL swallowed a genuine check failure (unreachable
                // responder/CRL, no revocation info available, etc) rather
                // than throwing -- the cert's status could not be
                // determined, which is not the same as "not revoked".
                return Result.UNKNOWN;
            }
            return Result.NOT_REVOKED;
        } catch (CertPathValidatorException e) {
            if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                return Result.REVOKED;
            }
            return Result.UNKNOWN;
        } catch (Exception e) {
            return Result.UNKNOWN;
        }
    }
}
