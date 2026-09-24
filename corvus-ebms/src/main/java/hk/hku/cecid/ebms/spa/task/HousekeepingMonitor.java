package hk.hku.cecid.ebms.spa.task;

import java.sql.Timestamp;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDAO;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.module.ActiveModule;

/**
 * Checks every minute (execution-interval in housekeeping.module.xml)
 * whether the daily housekeeping run is due, and runs it.
 * 
 * @see MessageHousekeeper
 */
public class HousekeepingMonitor extends ActiveModule {

    public HousekeepingMonitor(String descriptorLocation, ClassLoader loader,
            boolean shouldInitialize) {
        super(descriptorLocation, loader, shouldInitialize);
    }

    public boolean execute() {
        try {
            HousekeepingDAO dao = (HousekeepingDAO) EbmsProcessor.core.dao
                    .createDAO(HousekeepingDAO.class);
            HousekeepingDVO settings = dao.load();
            long now = System.currentTimeMillis();
            if (MessageHousekeeper.isDue(settings, now)) {
                String result;
                try {
                    result = MessageHousekeeper.run(settings);
                } catch (DAOException e) {
                    EbmsProcessor.core.log.error("Housekeeping run failed", e);
                    result = "Failed: " + e.getMessage();
                }
                // reload so settings saved during the run aren't overwritten
                settings = dao.load();
                settings.setLastRun(new Timestamp(now));
                settings.setLastResult("Scheduled: " + result);
                dao.save(settings);
            }
        } catch (Exception e) {
            // e.g. the housekeeping table is missing; try again next time
            EbmsProcessor.core.log.debug("Housekeeping check skipped: " + e.getMessage());
        }
        return true;
    }
}
