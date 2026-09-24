package hk.hku.cecid.ebms.spa.task;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.HousekeepingDVO;
import hk.hku.cecid.ebms.spa.dao.InboxDAO;
import hk.hku.cecid.ebms.spa.dao.InboxDVO;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.dao.MessageServerDAO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Deletes old messages as the housekeeping settings say: those older than
 * the retention period, in the chosen box(es), of the chosen types, whose
 * status is one of the chosen final statuses. Whatever the settings, it
 * never deletes a message the MSH is still handling (only final statuses
 * are accepted) nor an inbound message still waiting in the inbox for the
 * back-end application to collect.
 *
 * The candidates are found with the Message History queries, so this works
 * with every database the history page does.
 */
public class MessageHousekeeper {

    /** Statuses after which the MSH no longer touches a message. */
    public static final String[] FINAL_STATUSES = {
            MessageClassifier.INTERNAL_STATUS_DELIVERED,
            MessageClassifier.INTERNAL_STATUS_PROCESSED,
            MessageClassifier.INTERNAL_STATUS_DELIVERY_FAILURE,
            MessageClassifier.INTERNAL_STATUS_PROCESSED_ERROR };

    public static final String[] MESSAGE_TYPES = {
            MessageClassifier.MESSAGE_TYPE_ORDER,
            MessageClassifier.MESSAGE_TYPE_ACKNOWLEDGEMENT,
            MessageClassifier.MESSAGE_TYPE_ERROR,
            MessageClassifier.MESSAGE_TYPE_PING,
            MessageClassifier.MESSAGE_TYPE_PONG,
            MessageClassifier.MESSAGE_TYPE_STATUS_REQUEST,
            MessageClassifier.MESSAGE_TYPE_STATUS_RESPONSE,
            MessageClassifier.MESSAGE_TYPE_PROCESSED_ERROR };

    private static final int BATCH = 500;

    private MessageHousekeeper() {
    }

    /**
     * @return today's scheduled run time, or null if the setting is not a
     *         valid "HH:mm".
     */
    public static Timestamp slotOf(HousekeepingDVO settings, long day) {
        String runTime = settings.getRunTime();
        if (runTime == null || !runTime.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(day);
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(runTime.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(runTime.substring(3, 5)));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTimeInMillis());
    }

    /**
     * Due once a day: when today's run time has passed and there has been no
     * run since. A server that was down at the run time catches up when it
     * is back the same day.
     */
    public static boolean isDue(HousekeepingDVO settings, long now) {
        Timestamp slot = slotOf(settings, now);
        if (!settings.isEnabled() || slot == null || now < slot.getTime()) {
            return false;
        }
        Timestamp lastRun = settings.getLastRun();
        return lastRun == null || lastRun.getTime() < slot.getTime();
    }

    /** @return when the schedule runs next, or null if it is off. */
    public static Timestamp nextRun(HousekeepingDVO settings, long now) {
        Timestamp slot = slotOf(settings, now);
        if (!settings.isEnabled() || slot == null) {
            return null;
        }
        if (isDue(settings, now)) {
            return new Timestamp(now);
        }
        if (now < slot.getTime()) {
            return slot;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(slot.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return new Timestamp(calendar.getTimeInMillis());
    }

    /** @return the messages created before this are old enough to delete. */
    public static Timestamp cutOff(HousekeepingDVO settings, long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_MONTH, -settings.getRetentionDays());
        return new Timestamp(calendar.getTimeInMillis());
    }

    /** @return how many messages a run now would delete (at most). */
    public static int preview(HousekeepingDVO settings) throws DAOException {
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao.createDAO(MessageDAO.class);
        Timestamp cutOff = cutOff(settings, System.currentTimeMillis());
        int count = 0;
        for (Iterator i = criteria(settings, messageDAO).iterator(); i.hasNext();) {
            count += messageDAO.findNumberOfMessagesByHistory((MessageDVO) i.next(), null, cutOff);
        }
        return count;
    }

    /**
     * Deletes the messages the settings select. Runs one at a time, whether
     * started by the schedule or from the admin console.
     *
     * @return a one-line summary, e.g. "Deleted 120 message(s) older than ...".
     */
    public static synchronized String run(HousekeepingDVO settings) throws DAOException {
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao.createDAO(MessageDAO.class);
        MessageServerDAO serverDAO = (MessageServerDAO) EbmsProcessor.core.dao
                .createDAO(MessageServerDAO.class);
        InboxDAO inboxDAO = (InboxDAO) EbmsProcessor.core.dao.createDAO(InboxDAO.class);
        Timestamp cutOff = cutOff(settings, System.currentTimeMillis());

        int deleted = 0;
        int waiting = 0;
        int failed = 0;
        for (Iterator c = criteria(settings, messageDAO).iterator(); c.hasNext();) {
            MessageDVO criteria = (MessageDVO) c.next();
            // messages kept back stay in the result, so page past them
            int kept = 0;
            while (true) {
                List batch = messageDAO.findMessagesByHistory(criteria, null, cutOff, BATCH, kept);
                if (batch.isEmpty()) {
                    break;
                }
                for (Iterator m = batch.iterator(); m.hasNext();) {
                    MessageDVO message = (MessageDVO) m.next();
                    if (awaitsCollection(inboxDAO, message)) {
                        waiting++;
                        kept++;
                        continue;
                    }
                    try {
                        serverDAO.deleteMessage(message);
                        deleted++;
                    } catch (DAOException e) {
                        EbmsProcessor.core.log.error("Housekeeping: unable to delete message "
                                + message.getMessageId(), e);
                        failed++;
                        kept++;
                    }
                }
            }
        }

        String result = "Deleted " + deleted + " message(s) older than " + cutOff;
        if (waiting > 0) {
            result += "; kept " + waiting + " inbound message(s) not yet collected";
        }
        if (failed > 0) {
            result += "; " + failed + " could not be deleted (see ebms.log)";
        }
        EbmsProcessor.core.log.info("Housekeeping: " + result);
        return result;
    }

    /** An inbound message still in the inbox hasn't been collected yet. */
    private static boolean awaitsCollection(InboxDAO inboxDAO, MessageDVO message)
            throws DAOException {
        if (!MessageClassifier.MESSAGE_BOX_INBOX.equals(message.getMessageBox())) {
            return false;
        }
        InboxDVO inbox = (InboxDVO) inboxDAO.createDVO();
        inbox.setMessageId(message.getMessageId());
        return inboxDAO.findInbox(inbox);
    }

    /** One history query per box x type x status the settings select. */
    private static List criteria(HousekeepingDVO settings, MessageDAO messageDAO) {
        String[] boxes = HousekeepingDVO.ALL.equals(settings.getMessageBox())
                ? new String[] { null } : new String[] { settings.getMessageBox() };
        String[] types = HousekeepingDVO.ALL.equals(settings.getMessageTypes())
                ? new String[] { "%" } : split(settings.getMessageTypes());
        String[] statuses = finalStatuses(settings.getStatuses());

        List list = new ArrayList();
        for (int b = 0; b < boxes.length; b++) {
            for (int t = 0; t < types.length; t++) {
                for (int s = 0; s < statuses.length; s++) {
                    MessageDVO criteria = (MessageDVO) messageDAO.createDVO();
                    criteria.setMessageBox(boxes[b]);
                    criteria.setMessageType(types[t]);
                    criteria.setStatus(statuses[s]);
                    list.add(criteria);
                }
            }
        }
        return list;
    }

    /** The configured statuses that are final; anything else is ignored. */
    public static String[] finalStatuses(String statuses) {
        List list = new ArrayList();
        String[] configured = split(statuses);
        for (int i = 0; i < configured.length; i++) {
            for (int j = 0; j < FINAL_STATUSES.length; j++) {
                if (FINAL_STATUSES[j].equals(configured[i]) && !list.contains(configured[i])) {
                    list.add(configured[i]);
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    private static String[] split(String list) {
        if (list == null || list.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = list.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
