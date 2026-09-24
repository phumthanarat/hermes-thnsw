package hk.hku.cecid.piazza.corvus.core.main.admin.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * Storage of the admin console's security data: the audit log, each
 * user's password age/history and two-factor secret, and the security
 * settings.
 */
public interface ConsoleSecurityDAO extends DAO {

    /** One audit log entry. */
    class AuditEntry {
        public Timestamp time;
        public String username;
        public String clientIp;
        public String action;
        public String target;
        public String outcome;
        public String detail;
    }

    /** What is kept per user beyond Tomcat's user database. */
    class UserSecurity {
        public String username;
        public Timestamp passwordChanged;
        /** Previous password digests, newest first, space separated. */
        public String passwordHistory;
        public String totpSecret;
        public boolean totpEnabled;
        /** The last TOTP time step accepted (no code is accepted twice). */
        public Long totpLastStep;
    }

    void addAudit(AuditEntry entry) throws DAOException;

    /**
     * @param username exact user, or null for any
     * @param text     text in the action, target or detail, or null
     * @param from     earliest time (inclusive), or null
     * @param to       latest time (exclusive), or null
     */
    List<AuditEntry> findAudit(String username, String text, Timestamp from, Timestamp to,
            int limit, int offset) throws DAOException;

    int countAudit(String username, String text, Timestamp from, Timestamp to) throws DAOException;

    int purgeAudit(Timestamp before) throws DAOException;

    /** @return the user's record, or null if there is none yet. */
    UserSecurity getUserSecurity(String username) throws DAOException;

    void saveUserSecurity(UserSecurity security) throws DAOException;

    void removeUserSecurity(String username) throws DAOException;

    Map<String, String> getSettings() throws DAOException;

    void saveSetting(String name, String value) throws DAOException;
}
