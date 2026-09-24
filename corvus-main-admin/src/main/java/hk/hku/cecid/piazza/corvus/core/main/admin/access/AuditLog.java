package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;

/**
 * Records who did what in the admin console (Access > Audit Log). Never
 * fails the request it records: a failure to write is logged instead.
 * Entries older than the configured number of days are removed once a day.
 */
public final class AuditLog {

    public static final String OK = "ok";
    public static final String DENIED = "denied";
    public static final String FAILED = "failed";

    private static volatile long nextPurge;

    private AuditLog() {
    }

    public static void record(HttpServletRequest request, String username, String action,
            String target, String outcome, String detail) {
        ConsoleSecurityDAO.AuditEntry entry = new ConsoleSecurityDAO.AuditEntry();
        entry.time = new Timestamp(System.currentTimeMillis());
        entry.username = username != null ? username
                : request != null && request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        entry.clientIp = request == null ? null : request.getRemoteAddr();
        entry.action = action;
        entry.target = target;
        entry.outcome = outcome;
        entry.detail = detail;
        AdminMainProcessor.core.log.info("Audit: " + entry.username + " " + action
                + (target == null ? "" : " " + target) + " " + outcome
                + (detail == null ? "" : " (" + detail + ")"));
        try {
            ConsoleSecurityDAO dao = SecuritySettings.dao();
            dao.addAudit(entry);
            purgeOld(dao);
        } catch (Exception e) {
            AdminMainProcessor.core.log.error("Unable to write the audit log", e);
        }
    }

    public static void record(HttpServletRequest request, String action, String target,
            String outcome, String detail) {
        record(request, null, action, target, outcome, detail);
    }

    private static void purgeOld(ConsoleSecurityDAO dao) throws Exception {
        long now = System.currentTimeMillis();
        if (now < nextPurge) {
            return;
        }
        nextPurge = now + 24L * 3600 * 1000;
        int days = SecuritySettings.getInt(SecuritySettings.AUDIT_KEEP_DAYS);
        if (days > 0) {
            int removed = dao.purgeAudit(new Timestamp(now - days * 24L * 3600 * 1000));
            if (removed > 0) {
                AdminMainProcessor.core.log.info("Audit: removed " + removed + " entries older than "
                        + days + " days");
            }
        }
    }
}
