package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.dao.HttpChannelDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (HTTP port type)
 */
public class HttpChannelDSDVO extends DataSourceDVO implements HttpChannelDVO {

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

    public String getBindAddress() {
        return super.getString("bindAddress");
    }

    public void setBindAddress(String bindAddress) {
        super.setString("bindAddress", bindAddress);
    }

    public int getPort() {
        return super.getInt("port");
    }

    public void setPort(int port) {
        super.setInt("port", port);
    }

    public boolean isUseTls() {
        return super.getBoolean("useTls");
    }

    public void setIsUseTls(boolean useTls) {
        super.setBoolean("useTls", useTls);
    }

    public String getTargetService() {
        return super.getString("targetService");
    }

    public void setTargetService(String targetService) {
        super.setString("targetService", targetService);
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
