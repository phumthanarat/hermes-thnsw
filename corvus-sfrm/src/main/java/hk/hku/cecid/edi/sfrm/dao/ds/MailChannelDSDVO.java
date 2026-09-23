package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.dao.MailChannelDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (Mail port type)
 */
public class MailChannelDSDVO extends DataSourceDVO implements MailChannelDVO {

    public String getChannelId() {
        return super.getString("channelId");
    }

    public void setChannelId(String channelId) {
        super.setString("channelId", channelId);
    }

    public String getName() {
        return super.getString("name");
    }

    public void setName(String name) {
        super.setString("name", name);
    }

    public String getProtocol() {
        return super.getString("protocol");
    }

    public void setProtocol(String protocol) {
        super.setString("protocol", protocol);
    }

    public String getHost() {
        return super.getString("host");
    }

    public void setHost(String host) {
        super.setString("host", host);
    }

    public int getPort() {
        return super.getInt("port");
    }

    public void setPort(int port) {
        super.setInt("port", port);
    }

    public String getUsername() {
        return super.getString("username");
    }

    public void setUsername(String username) {
        super.setString("username", username);
    }

    public String getPasswordEncrypted() {
        return super.getString("passwordEncrypted");
    }

    public void setPasswordEncrypted(String passwordEncrypted) {
        super.setString("passwordEncrypted", passwordEncrypted);
    }

    public String getFolder() {
        return super.getString("folder");
    }

    public void setFolder(String folder) {
        super.setString("folder", folder);
    }

    public boolean isUseSsl() {
        return super.getBoolean("useSsl");
    }

    public void setIsUseSsl(boolean useSsl) {
        super.setBoolean("useSsl", useSsl);
    }

    public int getPollingInterval() {
        return super.getInt("pollingInterval");
    }

    public void setPollingInterval(int pollingInterval) {
        super.setInt("pollingInterval", pollingInterval);
    }

    public int getMaxMessagesPerPoll() {
        return super.getInt("maxMessagesPerPoll");
    }

    public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
        super.setInt("maxMessagesPerPoll", maxMessagesPerPoll);
    }

    public boolean isDisabled() {
        return super.getBoolean("isDisabled");
    }

    public void setIsDisabled(boolean isDisabled) {
        super.setBoolean("isDisabled", isDisabled);
    }

    public String getDescription() {
        return super.getString("description");
    }

    public void setDescription(String description) {
        super.setString("description", description);
    }

    public Timestamp getCreatedTimestamp() {
        return (Timestamp) super.get("createdTimestamp");
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        super.put("createdTimestamp", createdTimestamp);
    }
}
