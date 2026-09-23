package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.MailChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.MailChannelDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (Mail port type)
 */
public class MailChannelDSDAO extends DataSourceDAO implements MailChannelDAO {

    public DVO createDVO() {
        return new MailChannelDSDVO();
    }

    public MailChannelDVO findChannelById(String channelId) throws DAOException {
        MailChannelDVO dvo = (MailChannelDVO) createDVO();
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
