package hk.hku.cecid.edi.sfrm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloadsRepository;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.util.FTPCredentialCipher;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;

/**
 * SftpChannelTask connects to one configured remote SFTP server over SSH,
 * downloads ready ".sfrm" files from its remote_path, and funnels each one
 * into the local outgoing-payload-repository -- the same funnel target
 * every other channel type (local file polling, FTP, FTPS) uses, so the
 * already-proven send pipeline handles it identically.
 *
 * The server's SSH host key is pinned Trust-On-First-Use, the same model
 * an FTPS channel uses for its TLS certificate: the first key seen for a
 * channel is trusted and its fingerprint recorded, and any later
 * connection presenting a different key is refused.
 *
 * @author Hermes2+ (SFTP port type)
 */
public class SftpChannelTask extends ActiveTaskAdaptor {

    private static final int DEFAULT_MAX_FILES_PER_POLL = 10;
    private static final int CONNECT_TIMEOUT_MS = 30000;

    private final SftpChannelDVO channel;

    public SftpChannelTask(SftpChannelDVO channel) {
        this.channel = channel;
    }

    public void execute() throws Exception {
        TofuHostKeyRepository tofu = new TofuHostKeyRepository(channel.getHostKeyFingerprint());

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftp = null;
        try {
            String password = FTPCredentialCipher.decrypt(channel.getPasswordEncrypted());

            session = jsch.getSession(channel.getUsername(), channel.getHost(), channel.getPort());
            session.setPassword(password);
            jsch.setHostKeyRepository(tofu);
            session.setConfig("StrictHostKeyChecking", "yes");
            session.connect(CONNECT_TIMEOUT_MS);

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(CONNECT_TIMEOUT_MS);

            String remotePath = channel.getRemotePath();
            if (remotePath == null || remotePath.length() == 0) {
                remotePath = "/";
            }

            PackagedPayloadsRepository outgoingRepo = (PackagedPayloadsRepository)
                    SFRMProcessor.getInstance().getSystemModule().getComponent("outgoing-payload-repository");
            File targetDir = outgoingRepo.getRepository();

            int maxFiles = channel.getMaxFilesPerPoll();
            if (maxFiles <= 0) {
                maxFiles = DEFAULT_MAX_FILES_PER_POLL;
            }

            Vector entries = sftp.ls(remotePath);
            int downloaded = 0;
            for (int i = 0; i < entries.size() && downloaded < maxFiles; i++) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) entries.get(i);
                String filename = entry.getFilename();
                if (entry.getAttrs().isDir() || !isReady(filename)) {
                    continue;
                }

                File localTarget = new File(targetDir, filename);
                if (localTarget.exists()) {
                    // Same-named file already queued (e.g. from another channel); leave it for next poll.
                    continue;
                }

                String remoteFilePath = remotePath.endsWith("/") ? remotePath + filename : remotePath + "/" + filename;
                OutputStream out = new FileOutputStream(localTarget);
                try {
                    sftp.get(remoteFilePath, out);
                } finally {
                    out.close();
                }

                sftp.rm(remoteFilePath);
                downloaded++;
                SFRMProcessor.getInstance().getLogger().info(
                        "SFTP channel '" + channel.getName() + "' downloaded " + filename
                                + " into the outgoing repository");
            }

            persistNewlyTrustedHostKey(tofu);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public void onFailure(Throwable e) {
        SFRMProcessor.getInstance().getLogger().error(
                "SFTP channel '" + channel.getName() + "' task failed", e);
    }

    private void persistNewlyTrustedHostKey(TofuHostKeyRepository tofu) {
        if (tofu.getObservedFingerprint() == null) {
            return;
        }
        boolean hadNoPinnedKeyYet = channel.getHostKeyFingerprint() == null
                || channel.getHostKeyFingerprint().length() == 0;
        if (!hadNoPinnedKeyYet) {
            return;
        }
        try {
            channel.setHostKeyFingerprint(tofu.getObservedFingerprint());
            SftpChannelDAO dao = (SftpChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(SftpChannelDAO.class);
            dao.persist(channel);
            SFRMProcessor.getInstance().getLogger().info(
                    "SFTP channel '" + channel.getName() + "' pinned host key on first connect: "
                            + tofu.getObservedFingerprint());
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error(
                    "SFTP channel '" + channel.getName() + "' connected but failed to persist the pinned host key",
                    e);
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
     * Trust-On-First-Use HostKeyRepository: the first host key seen for a
     * channel is accepted and its SHA-256 fingerprint recorded; on every
     * later connection, the presented key must match the pinned
     * fingerprint or JSch is told CHANGED, which refuses the connection.
     */
    private static class TofuHostKeyRepository implements HostKeyRepository {

        private final String expectedFingerprint;
        private String observedFingerprint;

        TofuHostKeyRepository(String expectedFingerprint) {
            this.expectedFingerprint = expectedFingerprint;
        }

        public int check(String host, byte[] key) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                observedFingerprint = toHex(digest.digest(key));
            } catch (Exception e) {
                return NOT_INCLUDED;
            }

            if (expectedFingerprint == null || expectedFingerprint.length() == 0) {
                // First connection for this channel -- trust and record it.
                return OK;
            }
            if (expectedFingerprint.equalsIgnoreCase(observedFingerprint)) {
                return OK;
            }
            return CHANGED;
        }

        public void add(HostKey hostkey, UserInfo ui) {
            // Persistence is handled explicitly after a successful connection, not here.
        }

        public void remove(String host, String type) {
        }

        public void remove(String host, String type, byte[] key) {
        }

        public String getKnownHostsRepositoryID() {
            return "sfrm-sftp-channel-tofu";
        }

        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        public HostKey[] getHostKey(String host, String type) {
            return new HostKey[0];
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
