package hk.hku.cecid.piazza.corvus.core.main.admin.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * ListenerPortDSDVO is the data source implementation of ListenerPortDVO.
 */
public class ListenerPortDSDVO extends DataSourceDVO implements ListenerPortDVO {

    private static final long serialVersionUID = 1L;

    public String getPortId() {
        return super.getString("portId");
    }

    public void setPortId(String portId) {
        super.setString("portId", portId);
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

    public void setIsDisabled(boolean disabled) {
        super.setBoolean("isDisabled", disabled);
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
