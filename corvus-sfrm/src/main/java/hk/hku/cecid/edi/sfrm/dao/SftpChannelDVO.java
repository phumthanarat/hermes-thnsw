package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * SftpChannelDVO represents one remote SFTP (SSH File Transfer Protocol)
 * server the SFRM file polling mechanism connects to on a schedule,
 * downloading ready ".sfrm" files from its remote_path and funneling them
 * into the local outgoing-payload-repository -- same funnel target as a
 * local file polling channel or an FTP/FTPS channel.
 *
 * SFTP is a different protocol from FTP/FTPS (it runs over SSH), so it is
 * modelled as its own channel type/table rather than reusing the FTP one.
 * Authentication here is password-based (matching what the admin UI
 * exposes); the password is stored RSA-encrypted the same way an FTP
 * channel's password is (see FTPCredentialCipher). The server's SSH host
 * key is pinned Trust-On-First-Use, the same model used for an FTPS
 * channel's TLS certificate.
 *
 * @author Hermes2+ (SFTP port type)
 */
public interface SftpChannelDVO extends DVO {

    public String getChannelId();

    public void setChannelId(String channelId);

    public String getName();

    public void setName(String name);

    public String getHost();

    public void setHost(String host);

    public int getPort();

    public void setPort(int port);

    public String getUsername();

    public void setUsername(String username);

    /** @return the RSA-encrypted password, base64 encoded. Use FTPCredentialCipher to decrypt. */
    public String getPasswordEncrypted();

    public void setPasswordEncrypted(String passwordEncrypted);

    public String getRemotePath();

    public void setRemotePath(String remotePath);

    /**
     * @return the SSH host key fingerprint trusted on first connect
     *         (Trust On First Use), or null/empty if this channel has
     *         never connected successfully yet.
     */
    public String getHostKeyFingerprint();

    public void setHostKeyFingerprint(String hostKeyFingerprint);

    public int getPollingInterval();

    public void setPollingInterval(int pollingInterval);

    public int getMaxFilesPerPoll();

    public void setMaxFilesPerPoll(int maxFilesPerPoll);

    public boolean isDisabled();

    public void setIsDisabled(boolean isDisabled);

    public String getDescription();

    public void setDescription(String description);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
