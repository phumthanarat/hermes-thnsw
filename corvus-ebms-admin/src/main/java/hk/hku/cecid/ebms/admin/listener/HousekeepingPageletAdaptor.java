package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDAO;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDVO;
import hk.hku.cecid.ebms.spa.task.MessageHousekeeper;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * Settings of the scheduled deletion of old messages, with a preview of
 * what a run would delete and a button to run it now.
 *
 * @see MessageHousekeeper
 */
public class HousekeepingPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/housekeeping", "");
        try {
            HousekeepingDAO dao = (HousekeepingDAO) EbmsProcessor.core.dao
                    .createDAO(HousekeepingDAO.class);
            HousekeepingDVO settings = dao.load();

            if ("post".equalsIgnoreCase(request.getMethod())) {
                String action = request.getParameter("request_action");
                if ("save".equals(action) || "preview".equals(action) || "run".equals(action)) {
                    String problem = readForm(request, settings);
                    if (problem != null) {
                        request.setAttribute(ATTR_MESSAGE, "Not saved: " + problem);
                    } else if ("save".equals(action)) {
                        dao.save(settings);
                        request.setAttribute(ATTR_MESSAGE, "Settings saved");
                    } else if ("preview".equals(action)) {
                        request.setAttribute(ATTR_MESSAGE, "With these settings a run now would delete up to "
                                + MessageHousekeeper.preview(settings) + " message(s); inbound ones not yet"
                                + " collected are kept (settings not saved yet)");
                    } else {
                        dao.save(settings);
                        String result = MessageHousekeeper.run(settings);
                        settings = dao.load();
                        settings.setLastRun(new Timestamp(System.currentTimeMillis()));
                        settings.setLastResult("Run now: " + result);
                        dao.save(settings);
                        request.setAttribute(ATTR_MESSAGE, result);
                    }
                }
            }
            render(settings, dom);
        } catch (DAOException e) {
            EbmsProcessor.core.log.error("Unable to process the housekeeping page", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to load the housekeeping settings: "
                    + e.getMessage() + " (does the database have the housekeeping table?)");
        }
        return dom.getSource();
    }

    /** @return what is wrong with the form, or null once settings holds it. */
    private String readForm(HttpServletRequest request, HousekeepingDVO settings) {
        int days;
        try {
            days = Integer.parseInt(request.getParameter("retention_days").trim());
        } catch (Exception e) {
            return "Keep messages for must be a number of days";
        }
        if (days < 1) {
            return "Keep messages for at least 1 day";
        }
        String runTime = request.getParameter("run_time");
        if (runTime == null || !runTime.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            return "Run time must be HH:mm";
        }
        String box = request.getParameter("message_box");
        if (!HousekeepingDVO.ALL.equals(box) && !"inbox".equals(box) && !"outbox".equals(box)) {
            return "Unknown message box";
        }
        String types = HousekeepingDVO.ALL.equals(request.getParameter("all_types"))
                ? HousekeepingDVO.ALL : join(request.getParameterValues("message_type"),
                        Arrays.asList(MessageHousekeeper.MESSAGE_TYPES));
        if (types.length() == 0) {
            return "Choose at least one message type";
        }
        String statuses = join(request.getParameterValues("status"),
                Arrays.asList(MessageHousekeeper.FINAL_STATUSES));
        if (statuses.length() == 0) {
            return "Choose at least one status";
        }
        settings.setEnabled("true".equals(request.getParameter("enabled")));
        settings.setRetentionDays(days);
        settings.setRunTime(runTime);
        settings.setMessageBox(box);
        settings.setMessageTypes(types);
        settings.setStatuses(statuses);
        return null;
    }

    /** The submitted values that are allowed, comma separated. */
    private String join(String[] values, List allowed) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; values != null && i < values.length; i++) {
            if (allowed.contains(values[i]) && out.indexOf(values[i]) < 0) {
                out.append(out.length() == 0 ? "" : ",").append(values[i]);
            }
        }
        return out.toString();
    }

    private void render(HousekeepingDVO settings, PropertyTree dom) {
        long now = System.currentTimeMillis();
        dom.setProperty("enabled", String.valueOf(settings.isEnabled()));
        dom.setProperty("retention_days", String.valueOf(settings.getRetentionDays()));
        dom.setProperty("run_time", nullToEmpty(settings.getRunTime()));
        dom.setProperty("message_box", nullToEmpty(settings.getMessageBox()));
        dom.setProperty("cut_off", MessageHousekeeper.cutOff(settings, now).toString());
        Timestamp next = MessageHousekeeper.nextRun(settings, now);
        dom.setProperty("next_run", next == null ? "" : next.toString());
        dom.setProperty("last_run", settings.getLastRun() == null ? "" : settings.getLastRun().toString());
        dom.setProperty("last_result", nullToEmpty(settings.getLastResult()));

        boolean allTypes = HousekeepingDVO.ALL.equals(settings.getMessageTypes());
        dom.setProperty("all_types", String.valueOf(allTypes));
        List types = Arrays.asList(nullToEmpty(settings.getMessageTypes()).split(","));
        for (int i = 0; i < MessageHousekeeper.MESSAGE_TYPES.length; i++) {
            String type = MessageHousekeeper.MESSAGE_TYPES[i];
            dom.setProperty("type[" + (i + 1) + "]/name", type);
            dom.setProperty("type[" + (i + 1) + "]/checked", String.valueOf(allTypes || types.contains(type)));
        }
        List statuses = Arrays.asList(MessageHousekeeper.finalStatuses(settings.getStatuses()));
        String[] labels = { "Delivered", "Processed", "Delivery Failure", "Processed Error" };
        for (int i = 0; i < MessageHousekeeper.FINAL_STATUSES.length; i++) {
            String status = MessageHousekeeper.FINAL_STATUSES[i];
            dom.setProperty("status[" + (i + 1) + "]/code", status);
            dom.setProperty("status[" + (i + 1) + "]/label", labels[i]);
            dom.setProperty("status[" + (i + 1) + "]/checked", String.valueOf(statuses.contains(status)));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
