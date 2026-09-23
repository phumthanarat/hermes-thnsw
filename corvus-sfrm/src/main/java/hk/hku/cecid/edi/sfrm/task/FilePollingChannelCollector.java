package hk.hku.cecid.edi.sfrm.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDAO;
import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * FilePollingChannelCollector loads the enabled rows from
 * sfrm_file_polling_channel every execution cycle and produces one
 * {@link FilePollingChannelTask} per channel.
 *
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public class FilePollingChannelCollector extends ActiveTaskList {

    public List getTaskList() {
        ArrayList taskList = new ArrayList();
        try {
            FilePollingChannelDAO dao = (FilePollingChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(FilePollingChannelDAO.class);
            List channels = dao.findEnabledChannels();
            for (Iterator i = channels.iterator(); i.hasNext(); ) {
                FilePollingChannelDVO channel = (FilePollingChannelDVO) i.next();
                taskList.add(new FilePollingChannelTask(channel));
            }
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to load file polling channels", e);
        }
        return taskList;
    }
}
