package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;

/**
 * Tomcat's LockOutRealm (server.xml), reached over JMX: which users are
 * locked out after too many failed sign-ins, unlocking them, and applying
 * the attempts/minutes of Access > Security Settings.
 */
public final class LockOut {

    private LockOut() {
    }

    private static ObjectName realm() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        for (ObjectName name : server.queryNames(new ObjectName("Catalina:type=Realm,*"), null)) {
            // Tomcat's model MBeans name the realm class in modelerType
            Object type;
            try {
                type = server.getAttribute(name, "modelerType");
            } catch (Exception e) {
                type = server.getMBeanInfo(name).getClassName();
            }
            if (String.valueOf(type).endsWith("LockOutRealm")) {
                return name;
            }
        }
        return null;
    }

    public static boolean isLocked(String username) {
        try {
            ObjectName realm = realm();
            return realm != null && Boolean.TRUE.equals(ManagementFactory.getPlatformMBeanServer()
                    .invoke(realm, "isLocked", new Object[] { username },
                            new String[] { String.class.getName() }));
        } catch (Exception e) {
            return false;
        }
    }

    public static void unlock(String username) throws Exception {
        ObjectName realm = realm();
        if (realm == null) {
            throw new IllegalStateException("no LockOutRealm");
        }
        ManagementFactory.getPlatformMBeanServer().invoke(realm, "unlock", new Object[] { username },
                new String[] { String.class.getName() });
    }

    /** Applies the lock-out settings to the running realm. */
    public static void apply() {
        try {
            ObjectName realm = realm();
            if (realm == null) {
                return;
            }
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.setAttribute(realm, new Attribute("failureCount",
                    Integer.valueOf(SecuritySettings.getInt(SecuritySettings.LOCKOUT_ATTEMPTS))));
            server.setAttribute(realm, new Attribute("lockOutTime",
                    Integer.valueOf(SecuritySettings.getInt(SecuritySettings.LOCKOUT_MINUTES) * 60)));
        } catch (Exception e) {
            AdminMainProcessor.core.log.warn("Unable to apply the lock-out settings: " + e.getMessage());
        }
    }
}
