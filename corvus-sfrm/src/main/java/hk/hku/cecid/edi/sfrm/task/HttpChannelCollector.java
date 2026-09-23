package hk.hku.cecid.edi.sfrm.task;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.HttpChannelDAO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * HttpChannelCollector reconciles the standalone HTTP(S) listeners managed
 * by {@link HttpChannelListenerManager} against sfrm_http_channel every
 * execution cycle -- unlike the other port types, there is no per-channel
 * "poll for new work" step, opening/closing a listening socket to match
 * the current config *is* the work, so it is done directly here rather
 * than through a per-channel task.
 *
 * @author Hermes2+ (HTTP port type)
 */
public class HttpChannelCollector extends ActiveTaskList {

    public List getTaskList() {
        try {
            HttpChannelDAO dao = (HttpChannelDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(HttpChannelDAO.class);
            HttpChannelListenerManager.reconcile(dao.findEnabledChannels());
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to reconcile HTTP channels", e);
        }
        return new ArrayList();
    }
}
