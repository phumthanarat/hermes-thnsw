package hk.hku.cecid.hermes.api.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.hermes.api.dao.ApiKeyDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * ApiKeyDSDVO is the data source implementation of ApiKeyDVO.
 */
public class ApiKeyDSDVO extends DataSourceDVO implements ApiKeyDVO {

    private static final long serialVersionUID = 1L;

    public String getApiKey() {
        return super.getString("apiKey");
    }

    public void setApiKey(String apiKey) {
        super.setString("apiKey", apiKey);
    }

    public String getClientName() {
        return super.getString("clientName");
    }

    public void setClientName(String clientName) {
        super.setString("clientName", clientName);
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(super.getString("enabled"));
    }

    public void setEnabled(boolean enabled) {
        super.setString("enabled", enabled ? "true" : "false");
    }

    public Timestamp getCreatedTimestamp() {
        return (Timestamp) super.get("createdTimestamp");
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        super.put("createdTimestamp", createdTimestamp);
    }
}
