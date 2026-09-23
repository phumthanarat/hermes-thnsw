package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public class FilePollingChannelDSDVO extends DataSourceDVO implements FilePollingChannelDVO {

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

    public String getWatchPath() {
        return super.getString("watchPath");
    }

    public void setWatchPath(String watchPath) {
        super.setString("watchPath", watchPath);
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
