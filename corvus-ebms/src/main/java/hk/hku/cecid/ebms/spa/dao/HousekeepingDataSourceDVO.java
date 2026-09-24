package hk.hku.cecid.ebms.spa.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

public class HousekeepingDataSourceDVO extends DataSourceDVO implements HousekeepingDVO {

    public int getId() {
        return super.getInt("id");
    }

    public void setId(int id) {
        super.setInt("id", id);
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(super.getString("enabled"));
    }

    public void setEnabled(boolean enabled) {
        super.setString("enabled", String.valueOf(enabled));
    }

    public int getRetentionDays() {
        return super.getInt("retentionDays");
    }

    public void setRetentionDays(int retentionDays) {
        super.setInt("retentionDays", retentionDays);
    }

    public String getRunTime() {
        return super.getString("runTime");
    }

    public void setRunTime(String runTime) {
        super.setString("runTime", runTime);
    }

    public String getMessageBox() {
        return super.getString("messageBox");
    }

    public void setMessageBox(String messageBox) {
        super.setString("messageBox", messageBox);
    }

    public String getMessageTypes() {
        return super.getString("messageTypes");
    }

    public void setMessageTypes(String messageTypes) {
        super.setString("messageTypes", messageTypes);
    }

    public String getStatuses() {
        return super.getString("statuses");
    }

    public void setStatuses(String statuses) {
        super.setString("statuses", statuses);
    }

    public Timestamp getLastRun() {
        return super.getTimestamp("lastRun");
    }

    public void setLastRun(Timestamp lastRun) {
        super.setDate("lastRun", lastRun);
    }

    public String getLastResult() {
        return super.getString("lastResult");
    }

    public void setLastResult(String lastResult) {
        super.setString("lastResult", lastResult);
    }
}
