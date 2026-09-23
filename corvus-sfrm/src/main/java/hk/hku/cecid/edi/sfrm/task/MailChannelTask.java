package hk.hku.cecid.edi.sfrm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeUtility;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloadsRepository;
import hk.hku.cecid.edi.sfrm.dao.MailChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.util.FTPCredentialCipher;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;
import hk.hku.cecid.piazza.commons.net.MailReceiver;

/**
 * MailChannelTask logs into one configured mail account (POP3 or IMAP),
 * scans its folder for messages carrying a ready ".sfrm" attachment, and
 * funnels each one into the local outgoing-payload-repository -- the same
 * funnel target every other channel type (local file polling, FTP, FTPS,
 * SFTP) uses, so the already-proven send pipeline handles it identically.
 * Messages with no matching attachment are left untouched; messages that
 * yielded at least one funneled attachment are deleted (POP3) or marked
 * seen and deleted (IMAP) so they aren't reprocessed on the next poll.
 *
 * Several accounts can be configured as separate channels, each polling
 * independently on its own schedule.
 *
 * @author Hermes2+ (Mail port type)
 */
public class MailChannelTask extends ActiveTaskAdaptor {

    private static final int DEFAULT_MAX_MESSAGES_PER_POLL = 10;

    private final MailChannelDVO channel;

    public MailChannelTask(MailChannelDVO channel) {
        this.channel = channel;
    }

    public void execute() throws Exception {
        String protocol = channel.isUseSsl() ? channel.getProtocol() + "s" : channel.getProtocol();
        MailReceiver receiver = new MailReceiver(protocol, channel.getHost(), channel.getUsername(),
                FTPCredentialCipher.decrypt(channel.getPasswordEncrypted()));
        if (channel.getPort() > 0) {
            receiver.addProperty("mail." + channel.getProtocol() + ".port", String.valueOf(channel.getPort()));
        }

        Folder folder = null;
        try {
            receiver.connect();
            String folderName = channel.getFolder();
            if (folderName == null || folderName.length() == 0) {
                folderName = "INBOX";
            }
            folder = receiver.openFolder(folderName);

            PackagedPayloadsRepository outgoingRepo = (PackagedPayloadsRepository)
                    SFRMProcessor.getInstance().getSystemModule().getComponent("outgoing-payload-repository");
            File targetDir = outgoingRepo.getRepository();

            int maxMessages = channel.getMaxMessagesPerPoll();
            if (maxMessages <= 0) {
                maxMessages = DEFAULT_MAX_MESSAGES_PER_POLL;
            }

            Message[] messages = folder.getMessages();
            int processed = 0;
            for (int i = 0; i < messages.length && processed < maxMessages; i++) {
                Message message = messages[i];
                int funneled = funnelAttachments(message, targetDir);
                if (funneled > 0) {
                    message.setFlag(Flags.Flag.DELETED, true);
                    processed++;
                    SFRMProcessor.getInstance().getLogger().info(
                            "Mail channel '" + channel.getName() + "' funneled " + funneled
                                    + " attachment(s) from a message into the outgoing repository");
                }
            }
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(true);
            }
            try {
                receiver.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private int funnelAttachments(Message message, File targetDir) throws Exception {
        Object content = message.getContent();
        if (!(content instanceof Multipart)) {
            return 0;
        }
        Multipart multipart = (Multipart) content;
        int funneled = 0;
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || (part.getFileName() != null && !Part.INLINE.equalsIgnoreCase(disposition));
            if (!isAttachment) {
                continue;
            }
            String filename = part.getFileName();
            if (filename == null) {
                continue;
            }
            filename = MimeUtility.decodeText(filename);
            if (!isReady(filename)) {
                continue;
            }

            File localTarget = new File(targetDir, filename);
            if (localTarget.exists()) {
                // Same-named file already queued (e.g. from another channel); leave it for next poll.
                continue;
            }

            InputStream in = part.getInputStream();
            OutputStream out = new FileOutputStream(localTarget);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
                in.close();
            }
            funneled++;
        }
        return funneled;
    }

    public void onFailure(Throwable e) {
        SFRMProcessor.getInstance().getLogger().error(
                "Mail channel '" + channel.getName() + "' task failed", e);
    }

    private boolean isReady(String filename) {
        if (!filename.endsWith(".sfrm")) {
            return false;
        }
        char first = filename.charAt(0);
        return first != '~' && first != '.' && first != '#' && first != '%';
    }
}
