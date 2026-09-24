package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * HousekeepingDAO keeps the settings of the scheduled deletion of old
 * messages (a single row) and the outcome of its last run.
 */
public interface HousekeepingDAO extends DAO {

    /** @return the settings, or the defaults (disabled) if none are saved. */
    public HousekeepingDVO load() throws DAOException;

    public void save(HousekeepingDVO data) throws DAOException;
}
