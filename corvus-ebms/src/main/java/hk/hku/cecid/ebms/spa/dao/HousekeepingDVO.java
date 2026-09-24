package hk.hku.cecid.ebms.spa.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * Settings of the scheduled deletion of old messages. Lists (message types,
 * statuses) are comma separated; "all" means every message type.
 */
public interface HousekeepingDVO extends DVO {

    public static final String ALL = "all";

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    /** Messages older than this many days are deleted. */
    public int getRetentionDays();

    public void setRetentionDays(int retentionDays);

    /** Daily run time, "HH:mm" in the server's time zone. */
    public String getRunTime();

    public void setRunTime(String runTime);

    /** "inbox", "outbox" or ALL. */
    public String getMessageBox();

    public void setMessageBox(String messageBox);

    /** e.g. "Ping,Pong", or ALL. */
    public String getMessageTypes();

    public void setMessageTypes(String messageTypes);

    /** Final statuses only, e.g. "DL,PS". */
    public String getStatuses();

    public void setStatuses(String statuses);

    public Timestamp getLastRun();

    public void setLastRun(Timestamp lastRun);

    public String getLastResult();

    public void setLastResult(String lastResult);
}
