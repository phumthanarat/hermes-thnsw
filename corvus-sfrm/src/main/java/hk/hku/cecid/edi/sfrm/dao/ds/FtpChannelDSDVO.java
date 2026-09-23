package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.dao.FtpChannelDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (FTP port type)
 */
public class FtpChannelDSDVO extends DataSourceDVO implements FtpChannelDVO {

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

    public boolean isPassiveMode() {
        return super.getBoolean("isPassiveMode");
    }

    public void setIsPassiveMode(boolean isPassiveMode) {
        super.setBoolean("isPassiveMode", isPassiveMode);
    }

    public boolean isUseTls() {
        return super.getBoolean("useTls");
    }

    public void setIsUseTls(boolean useTls) {
        super.setBoolean("useTls", useTls);
    }

    public String getTlsCertFingerprint() {
        return super.getString("tlsCertFingerprint");
    }

    public void setTlsCertFingerprint(String tlsCertFingerprint) {
        super.setString("tlsCertFingerprint", tlsCertFingerprint);
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
