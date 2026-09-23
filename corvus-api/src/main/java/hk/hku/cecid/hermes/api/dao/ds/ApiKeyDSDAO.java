package hk.hku.cecid.hermes.api.dao.ds;

import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.hermes.api.dao.ApiKeyDAO;
import hk.hku.cecid.hermes.api.dao.ApiKeyDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * ApiKeyDSDAO is the data source implementation of ApiKeyDAO.
 */
public class ApiKeyDSDAO extends DataSourceDAO implements ApiKeyDAO {

    public DVO createDVO() {
        return new ApiKeyDSDVO();
    }

    public ApiKeyDVO findEnabledKey(String apiKey) throws DAOException {
        if (apiKey == null || apiKey.length() == 0) {
            return null;
        }
        Iterator iter = super.find("find_enabled_key", new Object[] { apiKey }).iterator();
        if (iter.hasNext()) {
            return (ApiKeyDVO) iter.next();
        }
        return null;
    }

    public List findAllKeys() throws DAOException {
        return super.find("find_all_keys", null);
    }
}
