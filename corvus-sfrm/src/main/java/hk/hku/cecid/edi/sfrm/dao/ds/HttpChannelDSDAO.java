package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.HttpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.HttpChannelDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (HTTP port type)
 */
public class HttpChannelDSDAO extends DataSourceDAO implements HttpChannelDAO {

    public DVO createDVO() {
        return new HttpChannelDSDVO();
    }

    public HttpChannelDVO findChannelById(String channelId) throws DAOException {
        HttpChannelDVO dvo = (HttpChannelDVO) createDVO();
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
