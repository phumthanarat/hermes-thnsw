package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;

/** Access > Audit Log (Administrator): who did what, searchable, newest first. */
public class AuditLogPageletAdaptor extends AdminPageletAdaptor {

    static final int PAGE_SIZE = 50;

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/audit", "");
        Filter f = new Filter(request);
        int offset = Math.max(0, parseInt(request.getParameter("offset")));
        try {
            ConsoleSecurityDAO dao = SecuritySettings.dao();
            int total = dao.countAudit(f.user, f.text, f.from, f.to);
            List<ConsoleSecurityDAO.AuditEntry> entries = dao.findAudit(f.user, f.text, f.from, f.to,
                    PAGE_SIZE, offset);
            int i = 0;
            for (ConsoleSecurityDAO.AuditEntry e : entries) {
                String prefix = "entry[" + (++i) + "]/";
                dom.setProperty(prefix + "time", e.time == null ? "" : e.time.toString().replaceAll("\\.\\d+$", ""));
                dom.setProperty(prefix + "user", nz(e.username));
                dom.setProperty(prefix + "ip", nz(e.clientIp));
                dom.setProperty(prefix + "action", nz(e.action));
                dom.setProperty(prefix + "target", nz(e.target));
                dom.setProperty(prefix + "outcome", nz(e.outcome));
                dom.setProperty(prefix + "detail", nz(e.detail));
            }
            dom.setProperty("total", String.valueOf(total));
            dom.setProperty("offset", String.valueOf(offset));
            dom.setProperty("page_size", String.valueOf(PAGE_SIZE));
            dom.setProperty("query", f.query());
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to read the audit log", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to read the audit log: " + e.getMessage());
        }
        dom.setProperty("filter/user", nz(f.user));
        dom.setProperty("filter/text", nz(f.text));
        dom.setProperty("filter/from", nz(request.getParameter("from")));
        dom.setProperty("filter/to", nz(request.getParameter("to")));
        dom.setProperty("keep_days", SecuritySettings.get(SecuritySettings.AUDIT_KEEP_DAYS));
        return dom.getSource();
    }

    /** The search fields, shared with the CSV export. */
    static class Filter {
        final String user, text;
        final Timestamp from, to;
        private final HttpServletRequest request;

        Filter(HttpServletRequest request) {
            this.request = request;
            user = blankToNull(request.getParameter("user"));
            text = blankToNull(request.getParameter("text"));
            from = date(request.getParameter("from"), false);
            to = date(request.getParameter("to"), true);
        }

        String query() {
            StringBuilder q = new StringBuilder();
            for (String name : new String[] { "user", "text", "from", "to" }) {
                String value = request.getParameter(name);
                if (value != null && !value.isEmpty()) {
                    try {
                        q.append('&').append(name).append('=').append(java.net.URLEncoder.encode(value, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            return q.toString();
        }
    }

    /** "yyyy-MM-dd"; the "to" day is included whole. */
    static Timestamp date(String value, boolean endOfDay) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setLenient(false);
            long time = format.parse(value.trim()).getTime();
            return new Timestamp(endOfDay ? time + 24L * 3600 * 1000 : time);
        } catch (Exception e) {
            return null;
        }
    }

    static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    static String nz(String value) {
        return value == null ? "" : value;
    }
}
