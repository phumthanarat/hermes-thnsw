package hk.hku.cecid.piazza.corvus.core.main.admin.dao.ds;

import java.util.List;

import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * ListenerPortDSDAO is the data source implementation of ListenerPortDAO.
 */
public class ListenerPortDSDAO extends DataSourceDAO implements ListenerPortDAO {

    public DVO createDVO() {
        return new ListenerPortDSDVO();
    }

    public ListenerPortDVO findPortById(String portId) throws DAOException {
        ListenerPortDVO dvo = (ListenerPortDVO) createDVO();
        dvo.setPortId(portId);
        if (super.retrieve(dvo)) {
            return dvo;
        }
        return null;
    }

    public List findAllPorts() throws DAOException {
        return super.find("find_all_ports", null);
    }

    public List findEnabledPorts() throws DAOException {
        return super.find("find_enabled_ports", null);
    }
}
