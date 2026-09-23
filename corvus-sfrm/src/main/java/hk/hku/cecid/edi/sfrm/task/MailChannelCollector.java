package hk.hku.cecid.edi.sfrm.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.MailChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.MailChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * MailChannelCollector loads the enabled rows from sfrm_mail_channel every
 * execution cycle and produces one {@link MailChannelTask} per account.
 *
 * @author Hermes2+ (Mail port type)
 */
public class MailChannelCollector extends ActiveTaskList {

    public List getTaskList() {
        ArrayList taskList = new ArrayList();
        try {
            MailChannelDAO dao = (MailChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(MailChannelDAO.class);
            List channels = dao.findEnabledChannels();
            for (Iterator i = channels.iterator(); i.hasNext(); ) {
                MailChannelDVO channel = (MailChannelDVO) i.next();
                taskList.add(new MailChannelTask(channel));
            }
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to load mail channels", e);
        }
        return taskList;
    }
}
