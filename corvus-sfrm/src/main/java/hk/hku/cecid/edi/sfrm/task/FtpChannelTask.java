package hk.hku.cecid.edi.sfrm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloadsRepository;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.util.FTPCredentialCipher;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;

/**
 * FtpChannelTask connects to one configured remote FTP (or FTPS) server,
 * downloads ready ".sfrm" files from its remote_path, and funnels each one
 * into the local outgoing-payload-repository -- the same funnel target
 * FilePollingChannelTask uses for local directories, so a remote FTP drop
 * is handled by the exact same, already-proven send pipeline.
 *
 * When use_tls is set, the connection is made as FTPS (explicit AUTH TLS)
 * with the server certificate pinned Trust-On-First-Use: the fingerprint
 * seen on the first successful connection is stored on the channel, and
 * any later connection presenting a different certificate is refused
 * (protects against a MITM after that first connection, without requiring
 * the admin to pre-supply a fingerprint or import the cert into the JVM
 * truststore).
 *
 * @author Hermes2+ (FTP/FTPS port type)
 */
public class FtpChannelTask extends ActiveTaskAdaptor {

    private static final int DEFAULT_MAX_FILES_PER_POLL = 10;

    private final FtpChannelDVO channel;

    public FtpChannelTask(FtpChannelDVO channel) {
        this.channel = channel;
    }

    public void execute() throws Exception {
        TofuTrustManager tofu = null;
        FTPClient ftp;

        if (channel.isUseTls()) {
            tofu = new TofuTrustManager(channel.getTlsCertFingerprint());
            FTPSClient ftps = new SessionReuseFTPSClient(false, channel.getHost());
            ftps.setTrustManager(tofu);
            ftp = ftps;
        } else {
            ftp = new FTPClient();
        }

        try {
            ftp.connect(channel.getHost(), channel.getPort());
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                SFRMProcessor.getInstance().getLogger().error(
                        "FTP channel '" + channel.getName() + "' connect to " + channel.getHost() + ":"
                                + channel.getPort() + " refused, reply code " + reply);
                return;
            }

            String password = FTPCredentialCipher.decrypt(channel.getPasswordEncrypted());
            boolean loggedIn = ftp.login(channel.getUsername(), password);
            if (!loggedIn) {
                SFRMProcessor.getInstance().getLogger().error(
                        "FTP channel '" + channel.getName() + "' login failed for user " + channel.getUsername());
                return;
            }

            if (ftp instanceof FTPSClient) {
                // Protect the data channel too, not just the control channel.
                ((FTPSClient) ftp).execPBSZ(0);
                ((FTPSClient) ftp).execPROT("P");
            }

            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            if (channel.isPassiveMode()) {
                ftp.enterLocalPassiveMode();
            }

            String remotePath = channel.getRemotePath();
            if (remotePath == null || remotePath.length() == 0) {
                remotePath = "/";
            }
            if (!ftp.changeWorkingDirectory(remotePath)) {
                SFRMProcessor.getInstance().getLogger().warn(
                        "FTP channel '" + channel.getName() + "' cannot change to remote path " + remotePath);
                return;
            }

            PackagedPayloadsRepository outgoingRepo = (PackagedPayloadsRepository)
                    SFRMProcessor.getInstance().getSystemModule().getComponent("outgoing-payload-repository");
            File targetDir = outgoingRepo.getRepository();

            int maxFiles = channel.getMaxFilesPerPoll();
            if (maxFiles <= 0) {
                maxFiles = DEFAULT_MAX_FILES_PER_POLL;
            }

            FTPFile[] files = ftp.listFiles();
            int downloaded = 0;
            for (int i = 0; i < files.length && downloaded < maxFiles; i++) {
                FTPFile f = files[i];
                if (!f.isFile() || !isReady(f.getName())) {
                    continue;
                }

                File localTarget = new File(targetDir, f.getName());
                if (localTarget.exists()) {
                    // Same-named file already queued (e.g. from another channel); leave it for next poll.
                    continue;
                }

                OutputStream out = new FileOutputStream(localTarget);
                boolean ok;
                try {
                    ok = ftp.retrieveFile(f.getName(), out);
                } finally {
                    out.close();
                }

                if (ok) {
                    ftp.deleteFile(f.getName());
                    downloaded++;
                    SFRMProcessor.getInstance().getLogger().info(
                            "FTP channel '" + channel.getName() + "' downloaded " + f.getName()
                                    + " into the outgoing repository");
                } else {
                    localTarget.delete();
                    SFRMProcessor.getInstance().getLogger().error(
                            "FTP channel '" + channel.getName() + "' failed to retrieve " + f.getName());
                }
            }

            ftp.logout();

            persistNewlyTrustedFingerprint(tofu);
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void onFailure(Throwable e) {
        SFRMProcessor.getInstance().getLogger().error(
                "FTP channel '" + channel.getName() + "' task failed", e);
    }

    private void persistNewlyTrustedFingerprint(TofuTrustManager tofu) {
        if (tofu == null || tofu.getObservedFingerprint() == null) {
            return;
        }
        boolean hadNoPinnedFingerprintYet = channel.getTlsCertFingerprint() == null
                || channel.getTlsCertFingerprint().length() == 0;
        if (!hadNoPinnedFingerprintYet) {
            return;
        }
        try {
            channel.setTlsCertFingerprint(tofu.getObservedFingerprint());
            FtpChannelDAO dao = (FtpChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(FtpChannelDAO.class);
            dao.persist(channel);
            SFRMProcessor.getInstance().getLogger().info(
                    "FTP channel '" + channel.getName() + "' pinned server certificate on first connect: "
                            + tofu.getObservedFingerprint());
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error(
                    "FTP channel '" + channel.getName() + "' connected over TLS but failed to persist the "
                            + "pinned certificate fingerprint", e);
        }
    }

    private boolean isReady(String filename) {
        if (!filename.endsWith(".sfrm")) {
            return false;
        }
        char first = filename.charAt(0);
        return first != '~' && first != '.' && first != '#' && first != '%';
    }

    /**
     * Hardened FTPS servers (pure-ftpd, proftpd's mod_tls, vsftpd) require the
     * data connection's TLS session to be resumed from the control connection's
     * session -- a defence against a data/control connection splicing attack.
     * Apache commons-net (through at least 3.11.x) never implemented this, so
     * without it the data channel negotiates a *fresh* TLS session and the
     * server silently drops both connections. There is no public JSSE API to
     * seed a resumable session for a not-yet-open socket, so this reaches into
     * the JDK's internal session cache the same way the (never-merged) upstream
     * patch for this exact interop problem does.
     */
    private static class SessionReuseFTPSClient extends FTPSClient {

        private final String configuredHost;

        SessionReuseFTPSClient(boolean isImplicit, String configuredHost) {
            super(isImplicit);
            this.configuredHost = configuredHost;
        }

        @Override
        protected void _prepareDataSocket_(Socket socket) throws IOException {
            if (!(socket instanceof SSLSocket) || !(_socket_ instanceof SSLSocket)) {
                return;
            }
            SSLSession controlSession = ((SSLSocket) _socket_).getSession();
            if (controlSession == null || !controlSession.isValid()) {
                return;
            }
            SSLSessionContext context = controlSession.getSessionContext();
            if (context == null) {
                return;
            }
            try {
                Field cacheField = context.getClass().getDeclaredField("sessionHostPortCache");
                cacheField.setAccessible(true);
                Object cache = cacheField.get(context);
                java.lang.reflect.Method putMethod = cache.getClass().getMethod("put", Object.class, Object.class);
                putMethod.setAccessible(true);
                // The exact host string the JVM's internal session cache will be looked up
                // under isn't part of any public contract, so seed every string that could
                // plausibly be used (reverse-DNS name, raw IP, and the host the admin typed
                // in when configuring the channel) rather than guessing a single one.
                String[] candidateHosts = new String[] {
                    socket.getInetAddress().getHostName(),
                    socket.getInetAddress().getHostAddress(),
                    configuredHost
                };
                for (String host : candidateHosts) {
                    String key = String.format("%s:%s", host, socket.getPort()).toLowerCase(Locale.ROOT);
                    putMethod.invoke(cache, key, controlSession);
                }
            } catch (Exception e) {
                // Best-effort: JVMs that don't expose this internal cache fall back to a
                // fresh handshake, which is what happened before this workaround existed.
            }
        }
    }

    /**
     * Trust-On-First-Use X509TrustManager: the first certificate seen for a
     * channel is accepted and its SHA-256 fingerprint recorded; on every
     * later connection, the presented certificate must match the pinned
     * fingerprint or the handshake is refused.
     */
    private static class TofuTrustManager implements X509TrustManager {

        private final String expectedFingerprint;
        private String observedFingerprint;

        TofuTrustManager(String expectedFingerprint) {
            this.expectedFingerprint = expectedFingerprint;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("Client certificate authentication is not supported");
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("Server presented no certificate");
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                observedFingerprint = toHex(digest.digest(chain[0].getEncoded()));
            } catch (Exception e) {
                throw new CertificateException("Unable to compute server certificate fingerprint", e);
            }

            if (expectedFingerprint != null && expectedFingerprint.length() > 0) {
                if (!expectedFingerprint.equalsIgnoreCase(observedFingerprint)) {
                    throw new CertificateException(
                            "Server certificate fingerprint changed since it was first trusted -- "
                                    + "expected " + expectedFingerprint + " but got " + observedFingerprint
                                    + ". Refusing the connection (possible man-in-the-middle).");
                }
            }
            // Else: first connection for this channel -- trust and record it.
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        String getObservedFingerprint() {
            return observedFingerprint;
        }

        private static String toHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        }
    }
}
