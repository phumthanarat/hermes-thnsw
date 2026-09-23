package hk.hku.cecid.piazza.corvus.core.main.admin.task;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.listener.ListenerPortManager;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * ListenerPortCollector reconciles the standalone HTTP(S) listeners managed
 * by {@link ListenerPortManager} against the listener_port table every
 * execution cycle -- there is no per-port "poll for new work" step, opening/
 * closing a listening socket to match the current config *is* the work, so
 * it is done directly here rather than through a per-port task. The admin
 * pagelet also calls the manager directly on every add/toggle/delete, so
 * this cycle mainly exists to restore configured ports after a restart and
 * to self-heal from any drift.
 */
public class ListenerPortCollector extends ActiveTaskList {

    public List getTaskList() {
        try {
            ListenerPortDAO dao = (ListenerPortDAO)
                    AdminMainProcessor.core.dao.createDAO(ListenerPortDAO.class);
            ListenerPortManager.reconcile(dao.findEnabledPorts());
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to reconcile listener ports", e);
        }
        return new ArrayList();
    }
}
