package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

/**
 * ListenerPortsPageletAdaptor lets an administrator hand a partner a
 * dedicated address:port (e.g. https://203.0.113.10:5443) to reach this
 * gateway on -- separate from the shared admin/web port and from every
 * other partner's own dedicated port -- without opening up a new protocol.
 * Each configured port just forwards to one of the gateway's existing
 * internal services (ebMS/AS2/SFRM).
 *
 * Every add/toggle/delete here also calls {@link ListenerPortManager}
 * directly so the change takes effect immediately; {@link
 * hk.hku.cecid.piazza.corvus.core.main.admin.task.ListenerPortCollector}
 * additionally reconciles on a timer to restore configured ports after a
 * restart and to self-heal from any drift.
 */
public class ListenerPortsPageletAdaptor extends AdminPageletAdaptor {

    private static final String ROOT = "/listener_ports";

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree resultDom = new PropertyTree();
        resultDom.setProperty(ROOT, "");

        try {
            updatePorts(request);
            appendPorts(resultDom);
        } catch (Exception e) {
            request.setAttribute(ATTR_MESSAGE,
                    "Unable to process the request: " + e.getMessage());
            throw new RuntimeException(
                    "Error in processing listener ports pagelet", e);
        }

        return resultDom.getSource();
    }

    private void updatePorts(HttpServletRequest request) throws Exception {

        String requestAction = request.getParameter(REQ_PARAM_ACTION);

        if (!"post".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        ListenerPortDAO dao = (ListenerPortDAO)
                AdminMainProcessor.core.dao.createDAO(ListenerPortDAO.class);

        if ("add_listener_port".equalsIgnoreCase(requestAction)) {
            String portId = trimOrNull(request.getParameter("port_id"));
            String name = trimOrNull(request.getParameter("name"));
            String bindAddress = trimOrNull(request.getParameter("bind_address"));
            String portStr = trimOrNull(request.getParameter("port"));
            boolean useTls = request.getParameter("use_tls") != null;
            String targetService = trimOrNull(request.getParameter("target_service"));
            String description = request.getParameter("description");

            if (portId == null) {
                request.setAttribute(ATTR_MESSAGE, "Port ID cannot be empty");
                return;
            }
            if (name == null) {
                request.setAttribute(ATTR_MESSAGE, "Name cannot be empty");
                return;
            }
            if (!isPositiveInteger(portStr)) {
                request.setAttribute(ATTR_MESSAGE, "Port must be a positive integer");
                return;
            }
            if (!"ebms".equals(targetService) && !"as2".equals(targetService) && !"sfrm".equals(targetService)) {
                request.setAttribute(ATTR_MESSAGE, "Target Service must be one of ebms, as2, sfrm");
                return;
            }
            if (dao.findPortById(portId) != null) {
                request.setAttribute(ATTR_MESSAGE, "Port ID '" + portId + "' already exists");
                return;
            }

            ListenerPortDVO dvo = (ListenerPortDVO) dao.createDVO();
            dvo.setPortId(portId);
            dvo.setName(name);
            dvo.setBindAddress(bindAddress == null ? "0.0.0.0" : bindAddress);
            dvo.setPort(Integer.parseInt(portStr));
            dvo.setIsUseTls(useTls);
            dvo.setTargetService(targetService);
            dvo.setIsDisabled(false);
            dvo.setDescription(description);
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "Listener port '" + portId + "' added successfully");
            ListenerPortManager.reconcile(dao.findEnabledPorts());

        } else if ("toggle_listener_port".equalsIgnoreCase(requestAction)) {
            String portId = request.getParameter("port_id");
            ListenerPortDVO dvo = dao.findPortById(portId);
            if (dvo != null) {
                dvo.setIsDisabled(!dvo.isDisabled());
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE,
                        "Listener port '" + portId + "' " + (dvo.isDisabled() ? "disabled" : "enabled"));
                ListenerPortManager.reconcile(dao.findEnabledPorts());
            }

        } else if ("delete_listener_port".equalsIgnoreCase(requestAction)) {
            String portId = request.getParameter("port_id");
            ListenerPortDVO dvo = dao.findPortById(portId);
            if (dvo != null) {
                dao.remove(dvo);
                request.setAttribute(ATTR_MESSAGE, "Listener port '" + portId + "' deleted");
                ListenerPortManager.reconcile(dao.findEnabledPorts());
            }
        }
    }

    private void appendPorts(PropertyTree resultDom) throws DAOException {
        ListenerPortDAO dao = (ListenerPortDAO)
                AdminMainProcessor.core.dao.createDAO(ListenerPortDAO.class);
        List ports = dao.findAllPorts();

        int i = 1;
        for (Iterator it = ports.iterator(); it.hasNext(); i++) {
            ListenerPortDVO dvo = (ListenerPortDVO) it.next();
            String prefix = ROOT + "/ports/port[" + i + "]";
            resultDom.setProperty(prefix + "/port_id", dvo.getPortId());
            resultDom.setProperty(prefix + "/name", dvo.getName());
            resultDom.setProperty(prefix + "/bind_address", dvo.getBindAddress());
            resultDom.setProperty(prefix + "/port", String.valueOf(dvo.getPort()));
            resultDom.setProperty(prefix + "/use_tls", String.valueOf(dvo.isUseTls()));
            resultDom.setProperty(prefix + "/target_service", dvo.getTargetService());
            resultDom.setProperty(prefix + "/is_disabled", String.valueOf(dvo.isDisabled()));
            resultDom.setProperty(prefix + "/description",
                    dvo.getDescription() == null ? "" : dvo.getDescription());
        }
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private boolean isPositiveInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return false;
        }
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
