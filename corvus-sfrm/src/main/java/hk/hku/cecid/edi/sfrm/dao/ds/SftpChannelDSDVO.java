package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.dao.SftpChannelDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (SFTP port type)
 */
public class SftpChannelDSDVO extends DataSourceDVO implements SftpChannelDVO {

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

    public String getRemotePath() {
        return super.getString("remotePath");
    }

    public void setRemotePath(String remotePath) {
        super.setString("remotePath", remotePath);
    }

    public String getHostKeyFingerprint() {
        return super.getString("hostKeyFingerprint");
    }

    public void setHostKeyFingerprint(String hostKeyFingerprint) {
        super.setString("hostKeyFingerprint", hostKeyFingerprint);
    }

    public int getPollingInterval() {
        return super.getInt("pollingInterval");
    }

    public void setPollingInterval(int pollingInterval) {
        super.setInt("pollingInterval", pollingInterval);
    }

    public int getMaxFilesPerPoll() {
        return super.getInt("maxFilesPerPoll");
    }

    public void setMaxFilesPerPoll(int maxFilesPerPoll) {
        super.setInt("maxFilesPerPoll", maxFilesPerPoll);
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
