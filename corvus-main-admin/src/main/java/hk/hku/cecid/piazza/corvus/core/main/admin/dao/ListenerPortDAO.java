package hk.hku.cecid.piazza.corvus.core.main.admin.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * ListenerPortDAO is the data access object for managing additional
 * HTTP(S) listener ports -- e.g. a partner reaching this gateway at a
 * dedicated address:port distinct from the shared admin/web port.
 */
public interface ListenerPortDAO extends DAO {

    public ListenerPortDVO findPortById(String portId) throws DAOException;

    public List findAllPorts() throws DAOException;

    public List findEnabledPorts() throws DAOException;
}
