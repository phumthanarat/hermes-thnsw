package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.SftpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (SFTP port type)
 */
public class SftpChannelDSDAO extends DataSourceDAO implements SftpChannelDAO {

    public DVO createDVO() {
        return new SftpChannelDSDVO();
    }

    public SftpChannelDVO findChannelById(String channelId) throws DAOException {
        SftpChannelDVO dvo = (SftpChannelDVO) createDVO();
        dvo.setChannelId(channelId);
        if (super.retrieve(dvo)) {
            return dvo;
        }
        return null;
    }

    public List findAllChannels() throws DAOException {
        return super.find("find_all_channels", null);
    }

    public List findEnabledChannels() throws DAOException {
        return super.find("find_enabled_channels", null);
    }
}
