package hk.hku.cecid.ebms.spa.task;

import java.sql.Timestamp;
import java.util.Arrays;

import junit.framework.TestCase;

import hk.hku.cecid.ebms.spa.dao.HousekeepingDVO;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDataSourceDVO;

public class MessageHousekeeperTest extends TestCase {

    private HousekeepingDVO settings(boolean enabled, String runTime, String lastRun) {
        HousekeepingDVO s = new HousekeepingDataSourceDVO();
        s.setEnabled(enabled);
        s.setRunTime(runTime);
        s.setRetentionDays(30);
        if (lastRun != null) {
            s.setLastRun(Timestamp.valueOf(lastRun));
        }
        return s;
    }

    private long at(String time) {
        return Timestamp.valueOf(time).getTime();
    }

    public void testDueOnceADayAfterTheRunTime() {
        HousekeepingDVO never = settings(true, "02:00", null);
        assertFalse(MessageHousekeeper.isDue(never, at("2026-09-24 01:59:00")));
        assertTrue(MessageHousekeeper.isDue(never, at("2026-09-24 02:00:00")));
        // a server that was down at 02:00 catches up later that day
        assertTrue(MessageHousekeeper.isDue(never, at("2026-09-24 15:00:00")));

        HousekeepingDVO ranToday = settings(true, "02:00", "2026-09-24 02:00:30");
        assertFalse(MessageHousekeeper.isDue(ranToday, at("2026-09-24 23:59:00")));
        assertTrue(MessageHousekeeper.isDue(ranToday, at("2026-09-25 02:00:00")));

        // a manual run before the slot doesn't skip that day's scheduled run
        HousekeepingDVO ranEarly = settings(true, "02:00", "2026-09-24 01:00:00");
        assertTrue(MessageHousekeeper.isDue(ranEarly, at("2026-09-24 02:01:00")));
    }

    public void testNeverDueWhenOffOrMisconfigured() {
        assertFalse(MessageHousekeeper.isDue(settings(false, "02:00", null), at("2026-09-24 03:00:00")));
        assertFalse(MessageHousekeeper.isDue(settings(true, "25:00", null), at("2026-09-24 03:00:00")));
        assertFalse(MessageHousekeeper.isDue(settings(true, null, null), at("2026-09-24 03:00:00")));
        assertNull(MessageHousekeeper.nextRun(settings(false, "02:00", null), at("2026-09-24 03:00:00")));
    }

    public void testNextRun() {
        assertEquals(Timestamp.valueOf("2026-09-24 02:00:00"), MessageHousekeeper.nextRun(
                settings(true, "02:00", "2026-09-23 02:00:10"), at("2026-09-24 01:00:00")));
        assertEquals(Timestamp.valueOf("2026-09-25 02:00:00"), MessageHousekeeper.nextRun(
                settings(true, "02:00", "2026-09-24 02:00:10"), at("2026-09-24 09:00:00")));
    }

    public void testCutOff() {
        assertEquals(Timestamp.valueOf("2026-08-25 09:00:00"),
                MessageHousekeeper.cutOff(settings(true, "02:00", null), at("2026-09-24 09:00:00")));
    }

    public void testOnlyFinalStatusesAreKept() {
        assertEquals(Arrays.asList(new String[] { "DL", "PE" }),
                Arrays.asList(MessageHousekeeper.finalStatuses("DL, PD,PR,PE,DL,XX")));
        assertEquals(0, MessageHousekeeper.finalStatuses(null).length);
    }
}
