package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * MailChannelDVO represents one mail account (POP3 or IMAP) the SFRM polling
 * mechanism logs into on a schedule, downloading ready ".sfrm" attachments
 * from unread messages in its folder and funneling them into the local
 * outgoing-payload-repository, exactly like the FTP/SFTP/file polling
 * channels do. Several accounts can be configured, each polling independently.
 *
 * The password is stored RSA-encrypted (see FTPCredentialCipher) using the
 * key pair already provisioned for SFRM message security.
 *
 * @author Hermes2+ (Mail port type)
 */
public interface MailChannelDVO extends DVO {

    public String getChannelId();

    public void setChannelId(String channelId);

    public String getName();

    public void setName(String name);

    /** @return "pop3" or "imap". */
    public String getProtocol();

    public void setProtocol(String protocol);

    public String getHost();

    public void setHost(String host);

    public int getPort();

    public void setPort(int port);

    public String getUsername();

    public void setUsername(String username);

    /** @return the RSA-encrypted password, base64 encoded. Use FTPCredentialCipher to decrypt. */
    public String getPasswordEncrypted();

    public void setPasswordEncrypted(String passwordEncrypted);

    public String getFolder();

    public void setFolder(String folder);

    public boolean isUseSsl();

    public void setIsUseSsl(boolean useSsl);

    public int getPollingInterval();

    public void setPollingInterval(int pollingInterval);

    public int getMaxMessagesPerPoll();

    public void setMaxMessagesPerPoll(int maxMessagesPerPoll);

    public boolean isDisabled();

    public void setIsDisabled(boolean isDisabled);

    public String getDescription();

    public void setDescription(String description);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
