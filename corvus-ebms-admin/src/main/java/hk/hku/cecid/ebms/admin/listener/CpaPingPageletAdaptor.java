package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDAO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDVO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * Lists every CPA that has an enabled partnership, with the outcome of its
 * latest Ping, and sends Pings to one or several CPAs at once. A CPA needs
 * no Ping partnership of its own: the MSH borrows the settings of another
 * partnership of the same CPA.
 */
public class CpaPingPageletAdaptor extends AdminPageletAdaptor {

    /** How long a Ping request waits for the Pongs before showing the page. */
    private static final long REPLY_TIMEOUT_MILLIS = 15 * 1000;

    protected Source getCenterSource(HttpServletRequest request) {
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/cpa_ping", "");

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                sendPings(request);
            }
            listCpas(dom);
        } catch (Exception e) {
            EbmsProcessor.core.log.debug("Unable to process the ping page request", e);
            throw new RuntimeException("Unable to process the ping page request", e);
        }
        return dom.getSource();
    }

    /**
     * Sends a Ping to each CPA the form asked for: the one whose row button
     * was pressed ("ping_one"), or every ticked row ("ping_selected").
     */
    private void sendPings(HttpServletRequest request) {
        List rows = new ArrayList();
        if (request.getParameter("ping_one") != null) {
            rows.add(request.getParameter("ping_one"));
        } else if (request.getParameterValues("selected") != null) {
            String[] selected = request.getParameterValues("selected");
            for (int i = 0; i < selected.length; i++) {
                rows.add(selected[i]);
            }
        }
        if (rows.isEmpty()) {
            request.setAttribute(ATTR_MESSAGE, "No CPA selected");
            return;
        }

        List pingIds = new ArrayList();
        StringBuffer failures = new StringBuffer();
        for (Iterator i = rows.iterator(); i.hasNext();) {
            String row = (String) i.next();
            String cpaId = trim(request.getParameter("cpa_" + row));
            if (cpaId == null) {
                continue;
            }
            String fromPartyId = trim(request.getParameter("from_" + row));
            String toPartyId = trim(request.getParameter("to_" + row));
            try {
                pingIds.add(PingSender.send(cpaId,
                        fromPartyId == null ? PingSender.DEFAULT_FROM_PARTY_ID : fromPartyId,
                        toPartyId == null ? cpaId : toPartyId, request));
            } catch (Exception e) {
                EbmsProcessor.core.log.error("Unable to send Ping to CPA: " + cpaId, e);
                failures.append(failures.length() == 0 ? "" : "; ")
                        .append(cpaId).append(": ").append(e.getMessage());
            }
        }

        // wait for the replies so the page shows the outcome right away
        int ok = 0, error = 0, noReply = 0;
        try {
            Map results = PingSender.awaitResults(pingIds, REPLY_TIMEOUT_MILLIS);
            for (Iterator i = results.values().iterator(); i.hasNext();) {
                Object result = i.next();
                if (PingSender.RESULT_OK.equals(result)) {
                    ok++;
                } else if (PingSender.RESULT_WAITING.equals(result)) {
                    noReply++;
                } else {
                    error++;
                }
            }
        } catch (DAOException e) {
            EbmsProcessor.core.log.error("Unable to check the Ping results", e);
            noReply = pingIds.size();
        }

        String message = "Ping sent to " + pingIds.size() + " CPA(s): " + ok
                + " Pong, " + error + " error/failed";
        if (noReply > 0) {
            message += ", " + noReply + " no reply within "
                    + (REPLY_TIMEOUT_MILLIS / 1000) + "s (Refresh to check again)";
        }
        if (failures.length() > 0) {
            message += ". Not sent: " + failures;
        }
        request.setAttribute(ATTR_MESSAGE, message);
    }

    private void listCpas(PropertyTree dom) throws DAOException {
        PartnershipDAO partnershipDAO = (PartnershipDAO) EbmsProcessor.core.dao
                .createDAO(PartnershipDAO.class);
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);

        // CPA ID -> its enabled partnerships, in partnership ID order (the
        // first is the one AgreementHandler borrows when there's no Ping one)
        Map cpas = new TreeMap();
        for (Iterator i = partnershipDAO.findAllPartnerships().iterator(); i.hasNext();) {
            PartnershipDVO partnership = (PartnershipDVO) i.next();
            if ("true".equalsIgnoreCase(partnership.getDisabled())) {
                continue;
            }
            List list = (List) cpas.get(partnership.getCpaId());
            if (list == null) {
                list = new ArrayList();
                cpas.put(partnership.getCpaId(), list);
            }
            list.add(partnership);
        }

        int row = 0;
        for (Iterator i = cpas.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String cpaId = (String) entry.getKey();
            List partnerships = (List) entry.getValue();
            String prefix = "cpa[" + (++row) + "]/";

            PartnershipDVO route = (PartnershipDVO) partnerships.get(0);
            boolean hasPingPartnership = false;
            for (Iterator p = partnerships.iterator(); p.hasNext();) {
                PartnershipDVO partnership = (PartnershipDVO) p.next();
                if (MessageClassifier.SERVICE.equals(partnership.getService())
                        && MessageClassifier.ACTION_PING.equals(partnership.getAction())) {
                    route = partnership;
                    hasPingPartnership = true;
                    break;
                }
            }

            dom.setProperty(prefix + "row", String.valueOf(row));
            dom.setProperty(prefix + "cpa_id", cpaId);
            dom.setProperty(prefix + "partnership_count", String.valueOf(partnerships.size()));
            dom.setProperty(prefix + "has_ping_partnership", String.valueOf(hasPingPartnership));
            dom.setProperty(prefix + "route_partnership_id", nullToEmpty(route.getPartnershipId()));
            dom.setProperty(prefix + "endpoint", nullToEmpty(route.getTransportEndpoint()));

            setPartyIds(messageDAO, cpaId, prefix, dom);
            setLastPing(messageDAO, cpaId, prefix, dom);
        }
    }

    /**
     * Suggests party IDs from the latest message exchanged under the CPA:
     * an outbox message as is, or an inbox message the other way round.
     */
    private void setPartyIds(MessageDAO messageDAO, String cpaId,
            String prefix, PropertyTree dom) throws DAOException {
        String from = "";
        String to = "";
        MessageDVO outbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_OUTBOX, "%");
        MessageDVO inbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_INBOX, "%");
        if (outbound != null && outbound.getFromPartyId() != null) {
            from = outbound.getFromPartyId();
            to = nullToEmpty(outbound.getToPartyId());
        } else if (inbound != null && inbound.getToPartyId() != null) {
            from = inbound.getToPartyId();
            to = nullToEmpty(inbound.getFromPartyId());
        }
        dom.setProperty(prefix + "from_party_id", from);
        dom.setProperty(prefix + "to_party_id", to);
    }

    /**
     * Reports the latest Ping sent under the CPA and what came back:
     * ok (a Pong), error (an ebMS Error), failed (not delivered) or waiting.
     */
    private void setLastPing(MessageDAO messageDAO, String cpaId,
            String prefix, PropertyTree dom) throws DAOException {
        MessageDVO ping = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_OUTBOX, MessageClassifier.MESSAGE_TYPE_PING);
        if (ping == null) {
            dom.setProperty(prefix + "result", "never");
            return;
        }
        dom.setProperty(prefix + "ping_message_id", ping.getMessageId());
        dom.setProperty(prefix + "ping_time", ping.getTimeStamp().toString());

        String result = PingSender.result(messageDAO, ping);
        dom.setProperty(prefix + "result", result);
        MessageDVO reply = null;
        if (PingSender.RESULT_OK.equals(result)) {
            reply = PingSender.findReply(messageDAO, ping.getMessageId(),
                    MessageClassifier.MESSAGE_TYPE_PONG);
        } else if (PingSender.RESULT_ERROR.equals(result)) {
            reply = PingSender.findReply(messageDAO, ping.getMessageId(),
                    MessageClassifier.MESSAGE_TYPE_ERROR);
        } else if (PingSender.RESULT_FAILED.equals(result)) {
            // the innermost cause (e.g. UnknownHostException) says the most
            String description = nullToEmpty(ping.getStatusDescription());
            String[] causes = description.split("\\\\n\\s*|\n\\s*");
            dom.setProperty(prefix + "detail",
                    causes[causes.length - 1].replaceFirst("^(\\\\t)?by ", ""));
        }
        if (reply != null) {
            dom.setProperty(prefix + "reply_time", reply.getTimeStamp().toString());
        }
    }

    private MessageDVO latestMessage(MessageDAO messageDAO, String cpaId,
            String messageBox, String messageType) throws DAOException {
        MessageDVO criteria = (MessageDVO) messageDAO.createDVO();
        // cpa_id is matched with LIKE, so its wildcards must be literal
        criteria.setCpaId(cpaId.replace("_", "\\_").replace("%", "\\%"));
        criteria.setMessageBox(messageBox);
        criteria.setMessageType(messageType);
        List found = messageDAO.findMessagesByHistory(criteria, 1, 0);
        return found.isEmpty() ? null : (MessageDVO) found.get(0);
    }

    private static String trim(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }
        return value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
