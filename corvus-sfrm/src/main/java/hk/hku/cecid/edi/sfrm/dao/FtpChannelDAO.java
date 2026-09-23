package hk.hku.cecid.edi.sfrm.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * @author Hermes2+ (FTP port type)
 */
public interface FtpChannelDAO extends DAO {

    public FtpChannelDVO findChannelById(String channelId) throws DAOException;

    public List findAllChannels() throws DAOException;

    public List findEnabledChannels() throws DAOException;
}
