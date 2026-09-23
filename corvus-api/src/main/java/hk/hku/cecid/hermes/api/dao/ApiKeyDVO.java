package hk.hku.cecid.hermes.api.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * ApiKeyDVO represents a row in the api_key table, used for authenticating
 * REST API requests via the X-API-Key header.
 */
public interface ApiKeyDVO extends DVO {

    public String getApiKey();

    public void setApiKey(String apiKey);

    public String getClientName();

    public void setClientName(String clientName);

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
