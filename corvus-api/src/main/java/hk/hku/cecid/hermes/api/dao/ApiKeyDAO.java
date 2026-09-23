package hk.hku.cecid.hermes.api.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * ApiKeyDAO is the data access object for managing API keys.
 */
public interface ApiKeyDAO extends DAO {

    /**
     * Finds an enabled API key matching the given key value.
     *
     * @param apiKey the key value presented by the caller.
     * @return the ApiKeyDVO if a matching, enabled key is found; null otherwise.
     */
    public ApiKeyDVO findEnabledKey(String apiKey) throws DAOException;

    /**
     * Lists all API keys ordered by creation time.
     */
    public List findAllKeys() throws DAOException;
}
