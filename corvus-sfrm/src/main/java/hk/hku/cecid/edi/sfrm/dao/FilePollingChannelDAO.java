package hk.hku.cecid.edi.sfrm.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public interface FilePollingChannelDAO extends DAO {

    public FilePollingChannelDVO findChannelById(String channelId) throws DAOException;

    public List findAllChannels() throws DAOException;

    public List findEnabledChannels() throws DAOException;
}
