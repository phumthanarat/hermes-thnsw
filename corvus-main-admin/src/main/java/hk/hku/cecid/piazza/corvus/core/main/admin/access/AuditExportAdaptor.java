package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;

/** The Audit Log search as a CSV download (/admin/access/audit_export). */
public class AuditExportAdaptor extends HttpRequestAdaptor {

    private static final int MAX_ROWS = 100000;

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        AuditLogPageletAdaptor.Filter f = new AuditLogPageletAdaptor.Filter(request);
        try {
            List<ConsoleSecurityDAO.AuditEntry> entries = SecuritySettings.dao().findAudit(f.user, f.text,
                    f.from, f.to, MAX_ROWS, 0);
            AuditLog.record(request, "audit log export", null, AuditLog.OK, entries.size() + " entries");
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"hermes-audit-log.csv\"");
            PrintWriter out = response.getWriter();
            out.print('﻿'); // so spreadsheet programs read UTF-8
            out.print("time,user,client_ip,action,target,outcome,detail\r\n");
            for (ConsoleSecurityDAO.AuditEntry e : entries) {
                out.print(csv(e.time == null ? "" : e.time.toString()) + "," + csv(e.username) + ","
                        + csv(e.clientIp) + "," + csv(e.action) + "," + csv(e.target) + ","
                        + csv(e.outcome) + "," + csv(e.detail) + "\r\n");
            }
            out.flush();
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to export: " + e.getMessage());
            } catch (IOException ignored) {
                // already committed
            }
        }
        return null;
    }

    /** A CSV field, quoted, and neutralised against spreadsheet formulas. */
    static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
