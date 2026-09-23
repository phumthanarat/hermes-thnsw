package hk.hku.cecid.edi.sfrm.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.FtpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FtpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * FtpChannelCollector loads the enabled rows from sfrm_ftp_channel every
 * execution cycle and produces one {@link FtpChannelTask} per channel.
 *
 * @author Hermes2+ (FTP port type)
 */
public class FtpChannelCollector extends ActiveTaskList {

    public List getTaskList() {
        ArrayList taskList = new ArrayList();
        try {
            FtpChannelDAO dao = (FtpChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(FtpChannelDAO.class);
            List channels = dao.findEnabledChannels();
            for (Iterator i = channels.iterator(); i.hasNext(); ) {
                FtpChannelDVO channel = (FtpChannelDVO) i.next();
                taskList.add(new FtpChannelTask(channel));
            }
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to load FTP channels", e);
        }
        return taskList;
    }
}
