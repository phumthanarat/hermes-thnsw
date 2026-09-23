package hk.hku.cecid.edi.sfrm.admin.listener;

import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDVO;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDVO;
import hk.hku.cecid.edi.sfrm.dao.HttpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.HttpChannelDVO;
import hk.hku.cecid.edi.sfrm.dao.MailChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.MailChannelDVO;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.util.FTPCredentialCipher;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * FilePollingPageletAdaptor lets an administrator configure the directories
 * and polling behaviour that the SFRM outbound file-drop mechanism uses --
 * the same mechanism exercised by dropping a
 * "&lt;partnership_id&gt;$&lt;message_id&gt;[filename].sfrm" file into the
 * outgoing repository directory to submit a message without going through
 * the Web Service API.
 *
 * Settings are persisted directly into the deployed module descriptor XML
 * files (sfrm.module.core.xml, sfrm.outgoing.payloads.collector.xml), the
 * same approach EbmsPropertiesPageletAdaptor uses for the ebMS module --
 * changes take effect on next plugin restart and survive container
 * restarts, but not a fresh image rebuild/redeploy (plugins/ is not a
 * persistent volume).
 *
 * @author Hermes2+ (SFRM file polling settings)
 */
public class FilePollingPageletAdaptor extends AdminPageletAdaptor {

    private static final String REQ_PARAM_PROPERTY = "property:";

    private static final String ROOT = "/file_polling";
    private static final String XPATH_OUTGOING_LOCATION = ROOT + "/outgoing_location";
    private static final String XPATH_INCOMING_LOCATION = ROOT + "/incoming_location";
    private static final String XPATH_POLLING_INTERVAL = ROOT + "/polling_interval";
    private static final String XPATH_MAX_FILES_PER_POLL = ROOT + "/max_files_per_poll";
    private static final String XPATH_MAX_THREADS = ROOT + "/max_threads";

    private static final String CORE_COMPONENT_OUTGOING = "outgoing-payload-repository";
    private static final String CORE_COMPONENT_INCOMING = "incoming-payload-repository";

    private PropertyTree coreProperties;
    private PropertyTree collectorProperties;

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree resultDom = new PropertyTree();

        try {
            URL coreUrl = SFRMProcessor.getInstance().getModuleGroup()
                    .getSystemModule().getDescriptor();
            coreProperties = new PropertyTree(coreUrl);

            URL collectorUrl = SFRMProcessor.getInstance().getModuleGroup()
                    .getModule("sfrm.outgoing.payloads.collector").getDescriptor();
            collectorProperties = new PropertyTree(collectorUrl);

            updateSettings(request);
            updateChannels(request);
            updateFtpChannels(request);
            updateSftpChannels(request);
            updateMailChannels(request);
            updateHttpChannels(request);
            resultDom = getSettings();
            appendChannels(resultDom);
            appendFtpChannels(resultDom);
            appendSftpChannels(resultDom);
            appendMailChannels(resultDom);
            appendHttpChannels(resultDom);
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE,
                    "Unable to process the request: " + e.getMessage());
            throw new RuntimeException(
                    "Error in processing SFRM file polling pagelet", e);
        }

        return resultDom.getSource();
    }

    private void updateSettings(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())
                || !"update".equalsIgnoreCase(requestAction)) {
            return;
        }

        String outgoingLocation = request.getParameter(REQ_PARAM_PROPERTY + XPATH_OUTGOING_LOCATION);
        if (outgoingLocation == null || outgoingLocation.trim().length() == 0) {
            request.setAttribute(ATTR_MESSAGE, "Outgoing Location cannot be empty");
            return;
        }

        String incomingLocation = request.getParameter(REQ_PARAM_PROPERTY + XPATH_INCOMING_LOCATION);
        if (incomingLocation == null || incomingLocation.trim().length() == 0) {
            request.setAttribute(ATTR_MESSAGE, "Incoming Location cannot be empty");
            return;
        }

        String pollingInterval = request.getParameter(REQ_PARAM_PROPERTY + XPATH_POLLING_INTERVAL);
        if (!isPositiveInteger(pollingInterval)) {
            request.setAttribute(ATTR_MESSAGE, "Polling Interval must be a positive integer (ms)");
            return;
        }

        String maxFilesPerPoll = request.getParameter(REQ_PARAM_PROPERTY + XPATH_MAX_FILES_PER_POLL);
        if (!isPositiveInteger(maxFilesPerPoll)) {
            request.setAttribute(ATTR_MESSAGE, "Max Files Per Poll must be a positive integer");
            return;
        }

        String maxThreads = request.getParameter(REQ_PARAM_PROPERTY + XPATH_MAX_THREADS);
        if (!isPositiveInteger(maxThreads)) {
            request.setAttribute(ATTR_MESSAGE, "Max Concurrent Threads must be a positive integer");
            return;
        }

        coreProperties.setProperty(
                "/module/component[@id='" + CORE_COMPONENT_OUTGOING + "']/parameter[@name='location']/@value",
                outgoingLocation.trim());
        coreProperties.setProperty(
                "/module/component[@id='" + CORE_COMPONENT_INCOMING + "']/parameter[@name='location']/@value",
                incomingLocation.trim());

        collectorProperties.setProperty(
                "/module/parameters/parameter[@name='execution-interval']/@value", pollingInterval);
        collectorProperties.setProperty(
                "/module/component[@id='task-list']/parameter[@name='max-task-per-list']/@value", maxFilesPerPoll);
        collectorProperties.setProperty(
                "/module/component[@id='task-list']/parameter[@name='max-thread-count']/@value", maxThreads);

        coreProperties.store();
        collectorProperties.store();

        request.setAttribute(ATTR_MESSAGE,
                "File polling settings updated successfully. Restart the SFRM plugin (or the app server) for the new paths/interval to take effect.");
    }

    private PropertyTree getSettings() {
        PropertyTree resultDom = new PropertyTree();
        resultDom.setProperty(ROOT, "");

        String outgoingLocation = coreProperties.getProperty(
                "/module/component[@id='" + CORE_COMPONENT_OUTGOING + "']/parameter[@name='location']/@value");
        resultDom.setProperty(XPATH_OUTGOING_LOCATION, outgoingLocation);

        String incomingLocation = coreProperties.getProperty(
                "/module/component[@id='" + CORE_COMPONENT_INCOMING + "']/parameter[@name='location']/@value");
        resultDom.setProperty(XPATH_INCOMING_LOCATION, incomingLocation);

        String pollingInterval = collectorProperties.getProperty(
                "/module/parameters/parameter[@name='execution-interval']/@value");
        resultDom.setProperty(XPATH_POLLING_INTERVAL, pollingInterval);

        String maxFilesPerPoll = collectorProperties.getProperty(
                "/module/component[@id='task-list']/parameter[@name='max-task-per-list']/@value");
        resultDom.setProperty(XPATH_MAX_FILES_PER_POLL, maxFilesPerPoll);

        String maxThreads = collectorProperties.getProperty(
                "/module/component[@id='task-list']/parameter[@name='max-thread-count']/@value");
        resultDom.setProperty(XPATH_MAX_THREADS, maxThreads);

        return resultDom;
    }

    private boolean isPositiveInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return false;
        }
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateChannels(HttpServletRequest request) throws DAOException {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        FilePollingChannelDAO dao = (FilePollingChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(FilePollingChannelDAO.class);

        if ("add_channel".equalsIgnoreCase(requestAction)) {
            String channelId = trimOrNull(request.getParameter("channel_id"));
            String name = trimOrNull(request.getParameter("channel_name"));
            String watchPath = trimOrNull(request.getParameter("channel_watch_path"));
            String pollingIntervalStr = trimOrNull(request.getParameter("channel_polling_interval"));
            String maxFilesStr = trimOrNull(request.getParameter("channel_max_files_per_poll"));
            String description = request.getParameter("channel_description");

            if (channelId == null) {
                request.setAttribute(ATTR_MESSAGE, "Channel ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "Channel Name cannot be empty");
                return;
            }
            if (watchPath == null) {
                request.setAttribute(ATTR_MESSAGE, "Watch Path cannot be empty");
                return;
            }
            if (dao.findChannelById(channelId) != null) {
                request.setAttribute(ATTR_MESSAGE, "Channel ID '" + channelId + "' already exists");
                return;
            }

            FilePollingChannelDVO dvo = (FilePollingChannelDVO) dao.createDVO();
            dvo.setChannelId(channelId);
            dvo.setName(name);
            dvo.setWatchPath(watchPath);
            dvo.setPollingInterval(parseIntOrDefault(pollingIntervalStr, -1));
            dvo.setMaxFilesPerPoll(parseIntOrDefault(maxFilesStr, -1));
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "Channel '" + channelId + "' added successfully");

        } else if ("toggle_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            FilePollingChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "Channel '" + channelId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
            }

        } else if ("delete_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            FilePollingChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "Channel '" + channelId + "' deleted");
            }
        }
    }

    private void appendChannels(PropertyTree resultDom) throws DAOException {
        FilePollingChannelDAO dao = (FilePollingChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(FilePollingChannelDAO.class);
        List channels = dao.findAllChannels();

        int i = 1;
        for (Iterator it = channels.iterator(); it.hasNext(); i++) {
            FilePollingChannelDVO dvo = (FilePollingChannelDVO) it.next();
            String prefix = ROOT + "/channels/channel[" + i + "]";
            resultDom.setProperty(prefix + "/channel_id", dvo.getChannelId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/watch_path", dvo.getWatchPath());
            resultDom.setProperty(prefix + "/polling_interval",
                    dvo.getPollingInterval() > 0 ? String.valueOf(dvo.getPollingInterval()) : "default");
            resultDom.setProperty(prefix + "/max_files_per_poll",
                    dvo.getMaxFilesPerPoll() > 0 ? String.valueOf(dvo.getMaxFilesPerPoll()) : "default");
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void updateFtpChannels(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        FtpChannelDAO dao = (FtpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(FtpChannelDAO.class);

        if ("add_ftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = trimOrNull(request.getParameter("ftp_channel_id"));
            String name = trimOrNull(request.getParameter("ftp_name"));
            String host = trimOrNull(request.getParameter("ftp_host"));
            String portStr = trimOrNull(request.getParameter("ftp_port"));
            String username = trimOrNull(request.getParameter("ftp_username"));
            String password = request.getParameter("ftp_password");
            String remotePath = trimOrNull(request.getParameter("ftp_remote_path"));
            boolean passiveMode = request.getParameter("ftp_passive_mode") != null;
            boolean useTls = request.getParameter("ftp_use_tls") != null;
            String pollingIntervalStr = trimOrNull(request.getParameter("ftp_polling_interval"));
            String maxFilesStr = trimOrNull(request.getParameter("ftp_max_files_per_poll"));
            String description = request.getParameter("ftp_description");

            if (channelId == null) {
                request.setAttribute(ATTR_MESSAGE, "FTP Channel ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "FTP Channel Name cannot be empty");
                return;
            }
            if (host == null) {
                request.setAttribute(ATTR_MESSAGE, "FTP Host cannot be empty");
                return;
            }
            if (dao.findChannelById(channelId) != null) {
                request.setAttribute(ATTR_MESSAGE, "FTP Channel ID '" + channelId + "' already exists");
                return;
            }

            FtpChannelDVO dvo = (FtpChannelDVO) dao.createDVO();
            dvo.setChannelId(channelId);
            dvo.setName(name);
            dvo.setHost(host);
            dvo.setPort(parseIntOrDefault(portStr, 21));
            dvo.setUsername(username);
            dvo.setPasswordEncrypted(FTPCredentialCipher.encrypt(password));
            dvo.setRemotePath(remotePath == null ? "/" : remotePath);
            dvo.setIsPassiveMode(passiveMode);
            dvo.setIsUseTls(useTls);
            dvo.setTlsCertFingerprint(null);
            dvo.setPollingInterval(parseIntOrDefault(pollingIntervalStr, -1));
            dvo.setMaxFilesPerPoll(parseIntOrDefault(maxFilesStr, -1));
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "FTP channel '" + channelId + "' added successfully");

        } else if ("toggle_ftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            FtpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "FTP channel '" + channelId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
            }

        } else if ("delete_ftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            FtpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "FTP channel '" + channelId + "' deleted");
            }
        }
    }

    private void appendFtpChannels(PropertyTree resultDom) throws DAOException {
        FtpChannelDAO dao = (FtpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(FtpChannelDAO.class);
        List channels = dao.findAllChannels();

        int i = 1;
        for (Iterator it = channels.iterator(); it.hasNext(); i++) {
            FtpChannelDVO dvo = (FtpChannelDVO) it.next();
            String prefix = ROOT + "/ftp_channels/channel[" + i + "]";
            resultDom.setProperty(prefix + "/channel_id", dvo.getChannelId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/host", dvo.getHost());
            resultDom.setProperty(prefix + "/port", String.valueOf(dvo.getPort()));
            resultDom.setProperty(prefix + "/username", dvo.getUsername() == null ? "" : dvo.getUsername());
            resultDom.setProperty(prefix + "/remote_path", dvo.getRemotePath());
            resultDom.setProperty(prefix + "/passive_mode", String.valueOf(dvo.isPassiveMode()));
            resultDom.setProperty(prefix + "/use_tls", String.valueOf(dvo.isUseTls()));
            resultDom.setProperty(prefix + "/tls_pinned",
                    String.valueOf(dvo.getTlsCertFingerprint() != null && dvo.getTlsCertFingerprint().length() > 0));
            resultDom.setProperty(prefix + "/polling_interval",
                    dvo.getPollingInterval() > 0 ? String.valueOf(dvo.getPollingInterval()) : "default");
            resultDom.setProperty(prefix + "/max_files_per_poll",
                    dvo.getMaxFilesPerPoll() > 0 ? String.valueOf(dvo.getMaxFilesPerPoll()) : "default");
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }

    private void updateSftpChannels(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        SftpChannelDAO dao = (SftpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(SftpChannelDAO.class);

        if ("add_sftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = trimOrNull(request.getParameter("sftp_channel_id"));
            String name = trimOrNull(request.getParameter("sftp_name"));
            String host = trimOrNull(request.getParameter("sftp_host"));
            String portStr = trimOrNull(request.getParameter("sftp_port"));
            String username = trimOrNull(request.getParameter("sftp_username"));
            String password = request.getParameter("sftp_password");
            String remotePath = trimOrNull(request.getParameter("sftp_remote_path"));
            String pollingIntervalStr = trimOrNull(request.getParameter("sftp_polling_interval"));
            String maxFilesStr = trimOrNull(request.getParameter("sftp_max_files_per_poll"));
            String description = request.getParameter("sftp_description");

            if (channelId == null) {
                request.setAttribute(ATTR_MESSAGE, "SFTP Channel ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "SFTP Channel Name cannot be empty");
                return;
            }
            if (host == null) {
                request.setAttribute(ATTR_MESSAGE, "SFTP Host cannot be empty");
                return;
            }
            if (dao.findChannelById(channelId) != null) {
                request.setAttribute(ATTR_MESSAGE, "SFTP Channel ID '" + channelId + "' already exists");
                return;
            }

            SftpChannelDVO dvo = (SftpChannelDVO) dao.createDVO();
            dvo.setChannelId(channelId);
            dvo.setName(name);
            dvo.setHost(host);
            dvo.setPort(parseIntOrDefault(portStr, 22));
            dvo.setUsername(username);
            dvo.setPasswordEncrypted(FTPCredentialCipher.encrypt(password));
            dvo.setRemotePath(remotePath == null ? "/" : remotePath);
            dvo.setHostKeyFingerprint(null);
            dvo.setPollingInterval(parseIntOrDefault(pollingIntervalStr, -1));
            dvo.setMaxFilesPerPoll(parseIntOrDefault(maxFilesStr, -1));
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "SFTP channel '" + channelId + "' added successfully");

        } else if ("toggle_sftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            SftpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "SFTP channel '" + channelId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
            }

        } else if ("delete_sftp_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            SftpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "SFTP channel '" + channelId + "' deleted");
            }
        }
    }

    private void appendSftpChannels(PropertyTree resultDom) throws DAOException {
        SftpChannelDAO dao = (SftpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(SftpChannelDAO.class);
        List channels = dao.findAllChannels();

        int i = 1;
        for (Iterator it = channels.iterator(); it.hasNext(); i++) {
            SftpChannelDVO dvo = (SftpChannelDVO) it.next();
            String prefix = ROOT + "/sftp_channels/channel[" + i + "]";
            resultDom.setProperty(prefix + "/channel_id", dvo.getChannelId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/host", dvo.getHost());
            resultDom.setProperty(prefix + "/port", String.valueOf(dvo.getPort()));
            resultDom.setProperty(prefix + "/username", dvo.getUsername() == null ? "" : dvo.getUsername());
            resultDom.setProperty(prefix + "/remote_path", dvo.getRemotePath());
            resultDom.setProperty(prefix + "/host_key_pinned",
                    String.valueOf(dvo.getHostKeyFingerprint() != null && dvo.getHostKeyFingerprint().length() > 0));
            resultDom.setProperty(prefix + "/polling_interval",
                    dvo.getPollingInterval() > 0 ? String.valueOf(dvo.getPollingInterval()) : "default");
            resultDom.setProperty(prefix + "/max_files_per_poll",
                    dvo.getMaxFilesPerPoll() > 0 ? String.valueOf(dvo.getMaxFilesPerPoll()) : "default");
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }

    private void updateMailChannels(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        MailChannelDAO dao = (MailChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(MailChannelDAO.class);

        if ("add_mail_channel".equalsIgnoreCase(requestAction)) {
            String channelId = trimOrNull(request.getParameter("mail_channel_id"));
            String name = trimOrNull(request.getParameter("mail_name"));
            String protocol = trimOrNull(request.getParameter("mail_protocol"));
            String host = trimOrNull(request.getParameter("mail_host"));
            String portStr = trimOrNull(request.getParameter("mail_port"));
            String username = trimOrNull(request.getParameter("mail_username"));
            String password = request.getParameter("mail_password");
            String folder = trimOrNull(request.getParameter("mail_folder"));
            boolean useSsl = request.getParameter("mail_use_ssl") != null;
            String pollingIntervalStr = trimOrNull(request.getParameter("mail_polling_interval"));
            String maxMessagesStr = trimOrNull(request.getParameter("mail_max_messages_per_poll"));
            String description = request.getParameter("mail_description");

            if (channelId == null) {
                request.setAttribute(ATTR_MESSAGE, "Mail Channel ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "Mail Channel Name cannot be empty");
                return;
            }
            if (host == null) {
                request.setAttribute(ATTR_MESSAGE, "Mail Host cannot be empty");
                return;
            }
            if (!"pop3".equals(protocol) && !"imap".equals(protocol)) {
                request.setAttribute(ATTR_MESSAGE, "Mail Protocol must be POP3 or IMAP");
                return;
            }
            if (dao.findChannelById(channelId) != null) {
                request.setAttribute(ATTR_MESSAGE, "Mail Channel ID '" + channelId + "' already exists");
                return;
            }

            MailChannelDVO dvo = (MailChannelDVO) dao.createDVO();
            dvo.setChannelId(channelId);
            dvo.setName(name);
            dvo.setProtocol(protocol);
            dvo.setHost(host);
            dvo.setPort(parseIntOrDefault(portStr, -1));
            dvo.setUsername(username);
            dvo.setPasswordEncrypted(FTPCredentialCipher.encrypt(password));
            dvo.setFolder(folder == null ? "INBOX" : folder);
            dvo.setIsUseSsl(useSsl);
            dvo.setPollingInterval(parseIntOrDefault(pollingIntervalStr, -1));
            dvo.setMaxMessagesPerPoll(parseIntOrDefault(maxMessagesStr, -1));
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "Mail channel '" + channelId + "' added successfully");

        } else if ("toggle_mail_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            MailChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "Mail channel '" + channelId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
            }

        } else if ("delete_mail_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            MailChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "Mail channel '" + channelId + "' deleted");
            }
        }
    }

    private void appendMailChannels(PropertyTree resultDom) throws DAOException {
        MailChannelDAO dao = (MailChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(MailChannelDAO.class);
        List channels = dao.findAllChannels();

        int i = 1;
        for (Iterator it = channels.iterator(); it.hasNext(); i++) {
            MailChannelDVO dvo = (MailChannelDVO) it.next();
            String prefix = ROOT + "/mail_channels/channel[" + i + "]";
            resultDom.setProperty(prefix + "/channel_id", dvo.getChannelId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/protocol", dvo.getProtocol());
            resultDom.setProperty(prefix + "/host", dvo.getHost());
            resultDom.setProperty(prefix + "/port", dvo.getPort() > 0 ? String.valueOf(dvo.getPort()) : "default");
            resultDom.setProperty(prefix + "/username", dvo.getUsername() == null ? "" : dvo.getUsername());
            resultDom.setProperty(prefix + "/folder", dvo.getFolder());
            resultDom.setProperty(prefix + "/use_ssl", String.valueOf(dvo.isUseSsl()));
            resultDom.setProperty(prefix + "/polling_interval",
                    dvo.getPollingInterval() > 0 ? String.valueOf(dvo.getPollingInterval()) : "default");
            resultDom.setProperty(prefix + "/max_messages_per_poll",
                    dvo.getMaxMessagesPerPoll() > 0 ? String.valueOf(dvo.getMaxMessagesPerPoll()) : "default");
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }

    private void updateHttpChannels(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        HttpChannelDAO dao = (HttpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(HttpChannelDAO.class);

        if ("add_http_channel".equalsIgnoreCase(requestAction)) {
            String channelId = trimOrNull(request.getParameter("http_channel_id"));
            String name = trimOrNull(request.getParameter("http_name"));
            String bindAddress = trimOrNull(request.getParameter("http_bind_address"));
            String portStr = trimOrNull(request.getParameter("http_port"));
            boolean useTls = request.getParameter("http_use_tls") != null;
            String targetService = trimOrNull(request.getParameter("http_target_service"));
            String description = request.getParameter("http_description");

            if (channelId == null) {
                request.setAttribute(ATTR_MESSAGE, "HTTP Channel ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "HTTP Channel Name cannot be empty");
                return;
            }
            if (!isPositiveInteger(portStr)) {
                request.setAttribute(ATTR_MESSAGE, "HTTP Port must be a positive integer");
                return;
            }
            if (!"ebms".equals(targetService) && !"as2".equals(targetService) && !"sfrm".equals(targetService)) {
                request.setAttribute(ATTR_MESSAGE, "HTTP Target Service must be one of ebms, as2, sfrm");
                return;
            }
            if (dao.findChannelById(channelId) != null) {
                request.setAttribute(ATTR_MESSAGE, "HTTP Channel ID '" + channelId + "' already exists");
                return;
            }

            HttpChannelDVO dvo = (HttpChannelDVO) dao.createDVO();
            dvo.setChannelId(channelId);
            dvo.setName(name);
            dvo.setBindAddress(bindAddress == null ? "0.0.0.0" : bindAddress);
            dvo.setPort(Integer.parseInt(portStr));
            dvo.setIsUseTls(useTls);
            dvo.setTargetService(targetService);
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "HTTP channel '" + channelId + "' added successfully");

        } else if ("toggle_http_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            HttpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "HTTP channel '" + channelId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
            }

        } else if ("delete_http_channel".equalsIgnoreCase(requestAction)) {
            String channelId = request.getParameter("channel_id");
            HttpChannelDVO dvo = dao.findChannelById(channelId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "HTTP channel '" + channelId + "' deleted");
            }
        }
    }

    private void appendHttpChannels(PropertyTree resultDom) throws DAOException {
        HttpChannelDAO dao = (HttpChannelDAO)
                SFRMProcessor.getInstance().getDAOFactory().createDAO(HttpChannelDAO.class);
        List channels = dao.findAllChannels();

        int i = 1;
        for (Iterator it = channels.iterator(); it.hasNext(); i++) {
            HttpChannelDVO dvo = (HttpChannelDVO) it.next();
            String prefix = ROOT + "/http_channels/channel[" + i + "]";
            resultDom.setProperty(prefix + "/channel_id", dvo.getChannelId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/bind_address", dvo.getBindAddress());
            resultDom.setProperty(prefix + "/port", String.valueOf(dvo.getPort()));
            resultDom.setProperty(prefix + "/use_tls", String.valueOf(dvo.isUseTls()));
            resultDom.setProperty(prefix + "/target_service", dvo.getTargetService());
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }
}
