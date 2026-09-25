package hk.hku.cecid.piazza.corvus.core.main.admin.dao.ds;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;
import hk.hku.cecid.piazza.commons.dao.ds.NullableObject;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ConsoleSecurityDAO;

/**
 * ConsoleSecurityDAO over the SQL statements named in admin.dao.xml, so
 * each database gets its own dialect there.
 */
public class ConsoleSecurityDSDAO extends DataSourceDAO implements ConsoleSecurityDAO {

    public DVO createDVO() {
        throw new UnsupportedOperationException("ConsoleSecurityDAO has no DVO");
    }

    public void addAudit(AuditEntry e) throws DAOException {
        executeUpdate(getSQL("audit_insert"), new Object[] { e.time,
                new NullableObject(e.username, Types.VARCHAR), new NullableObject(e.clientIp, Types.VARCHAR),
                truncate(e.action, 100), new NullableObject(truncate(e.target, 500), Types.VARCHAR),
                e.outcome, new NullableObject(truncate(e.detail, 1000), Types.VARCHAR) });
    }

    public List<AuditEntry> findAudit(String username, String text, Timestamp from, Timestamp to,
            int limit, int offset) throws DAOException {
        List<Object> params = new ArrayList<Object>();
        String sql = getSQL("audit_select") + where(username, text, from, to, params) + " "
                + getSQL("audit_page");
        // LIMIT ? OFFSET ?, or OFFSET ? ROWS FETCH NEXT ? ROWS ONLY (Oracle)
        if ("true".equals(getParameters().getProperty("page_offset_first"))) {
            params.add(Integer.valueOf(offset));
            params.add(Integer.valueOf(limit));
        } else {
            params.add(Integer.valueOf(limit));
            params.add(Integer.valueOf(offset));
        }
        List<AuditEntry> entries = new ArrayList<AuditEntry>();
        for (Iterator<?> i = executeRawQuery(sql, params.toArray()).iterator(); i.hasNext();) {
            List<?> row = (List<?>) i.next();
            AuditEntry e = new AuditEntry();
            e.time = toTimestamp(row.get(0));
            e.username = (String) row.get(1);
            e.clientIp = (String) row.get(2);
            e.action = (String) row.get(3);
            e.target = (String) row.get(4);
            e.outcome = (String) row.get(5);
            e.detail = (String) row.get(6);
            entries.add(e);
        }
        return entries;
    }

    public int countAudit(String username, String text, Timestamp from, Timestamp to)
            throws DAOException {
        List<Object> params = new ArrayList<Object>();
        List<?> rows = executeRawQuery(getSQL("audit_count") + where(username, text, from, to, params),
                params.toArray());
        return ((Number) ((List<?>) rows.get(0)).get(0)).intValue();
    }

    private String where(String username, String text, Timestamp from, Timestamp to, List<Object> params) {
        StringBuilder where = new StringBuilder();
        if (username != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("username = ?");
            params.add(username);
        }
        if (text != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ")
                    .append("(lower(action) LIKE ? OR lower(target) LIKE ? OR lower(detail) LIKE ?)");
            String like = "%" + text.toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (from != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("event_time >= ?");
            params.add(from);
        }
        if (to != null) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("event_time < ?");
            params.add(to);
        }
        return where.toString();
    }

    public int purgeAudit(Timestamp before) throws DAOException {
        return executeUpdate(getSQL("audit_purge"), new Object[] { before });
    }

    public UserSecurity getUserSecurity(String username) throws DAOException {
        List<?> rows = executeRawQuery(getSQL("security_select"), new Object[] { username });
        if (rows.isEmpty()) {
            return null;
        }
        List<?> row = (List<?>) rows.get(0);
        UserSecurity s = new UserSecurity();
        s.username = username;
        s.passwordChanged = toTimestamp(row.get(0));
        s.passwordHistory = (String) row.get(1);
        s.totpSecret = (String) row.get(2);
        s.totpEnabled = "true".equalsIgnoreCase(String.valueOf(row.get(3)));
        s.totpLastStep = row.get(4) == null ? null : Long.valueOf(((Number) row.get(4)).longValue());
        return s;
    }

    public void saveUserSecurity(UserSecurity s) throws DAOException {
        // typed nulls: not every driver can infer a null parameter's type
        Object[] params = { new NullableObject(s.passwordChanged, Types.TIMESTAMP),
                new NullableObject(s.passwordHistory, Types.VARCHAR),
                new NullableObject(s.totpSecret, Types.VARCHAR), String.valueOf(s.totpEnabled),
                new NullableObject(s.totpLastStep, Types.BIGINT), s.username };
        if (executeUpdate(getSQL("security_update"), params) == 0) {
            executeUpdate(getSQL("security_insert"), params);
        }
    }

    public void removeUserSecurity(String username) throws DAOException {
        executeUpdate(getSQL("security_delete"), new Object[] { username });
    }

    public Map<String, String> getSettings() throws DAOException {
        Map<String, String> settings = new HashMap<String, String>();
        for (Iterator<?> i = executeRawQuery(getSQL("setting_select_all"), null).iterator(); i.hasNext();) {
            List<?> row = (List<?>) i.next();
            settings.put((String) row.get(0), (String) row.get(1));
        }
        return settings;
    }

    public void saveSetting(String name, String value) throws DAOException {
        if (executeUpdate(getSQL("setting_update"), new Object[] { value, name }) == 0) {
            executeUpdate(getSQL("setting_insert"), new Object[] { value, name });
        }
    }

    private static Timestamp toTimestamp(Object value) {
        if (value == null || value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        if (value instanceof java.time.LocalDateTime) {
            return Timestamp.valueOf((java.time.LocalDateTime) value);
        }
        // e.g. Oracle's own TIMESTAMP type
        try {
            return (Timestamp) value.getClass().getMethod("timestampValue").invoke(value);
        } catch (Exception e) {
            return Timestamp.valueOf(value.toString());
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
