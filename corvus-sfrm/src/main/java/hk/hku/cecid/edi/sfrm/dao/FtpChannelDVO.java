package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * FtpChannelDVO represents one remote FTP server the SFRM file polling
 * mechanism connects to on a schedule, downloading ready ".sfrm" files from
 * its remote_path and funneling them into the local outgoing-payload-
 * repository, exactly like a local file polling channel does for a local
 * directory.
 *
 * The password is stored RSA-encrypted (see FTPCredentialCipher) using the
 * key pair already provisioned for SFRM message security.
 *
 * @author Hermes2+ (FTP port type)
 */
public interface FtpChannelDVO extends DVO {

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

    public boolean isPassiveMode();

    public void setIsPassiveMode(boolean isPassiveMode);

    /** @return true if this channel should connect via FTPS (explicit AUTH TLS). */
    public boolean isUseTls();

    public void setIsUseTls(boolean useTls);

    /**
     * @return the SHA-256 fingerprint of the server certificate trusted on
     *         first connect (Trust On First Use), or null/empty if this
     *         channel has never connected successfully yet.
     */
    public String getTlsCertFingerprint();

    public void setTlsCertFingerprint(String tlsCertFingerprint);

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
