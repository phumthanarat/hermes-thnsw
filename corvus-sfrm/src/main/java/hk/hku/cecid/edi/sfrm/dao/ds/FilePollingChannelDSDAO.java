package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public class FilePollingChannelDSDAO extends DataSourceDAO implements FilePollingChannelDAO {

    public DVO createDVO() {
        return new FilePollingChannelDSDVO();
    }

    public FilePollingChannelDVO findChannelById(String channelId) throws DAOException {
        FilePollingChannelDVO dvo = (FilePollingChannelDVO) createDVO();
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
