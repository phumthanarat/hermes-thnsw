package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.FtpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (FTP port type)
 */
public class FtpChannelDSDAO extends DataSourceDAO implements FtpChannelDAO {

    public DVO createDVO() {
        return new FtpChannelDSDVO();
    }

    public FtpChannelDVO findChannelById(String channelId) throws DAOException {
        FtpChannelDVO dvo = (FtpChannelDVO) createDVO();
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
