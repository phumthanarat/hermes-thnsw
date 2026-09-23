package hk.hku.cecid.edi.sfrm.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.SftpChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.SftpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * SftpChannelCollector loads the enabled rows from sfrm_sftp_channel every
 * execution cycle and produces one {@link SftpChannelTask} per channel.
 *
 * @author Hermes2+ (SFTP port type)
 */
public class SftpChannelCollector extends ActiveTaskList {

    public List getTaskList() {
        ArrayList taskList = new ArrayList();
        try {
            SftpChannelDAO dao = (SftpChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(SftpChannelDAO.class);
            List channels = dao.findEnabledChannels();
            for (Iterator i = channels.iterator(); i.hasNext(); ) {
                SftpChannelDVO channel = (SftpChannelDVO) i.next();
                taskList.add(new SftpChannelTask(channel));
            }
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to load SFTP channels", e);
        }
        return taskList;
    }
}
