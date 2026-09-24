package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.pkg.EbxmlMessage;
import hk.hku.cecid.ebms.pkg.MessageHeader;
import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.EbmsUtility;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.ebms.spa.handler.MessageServiceHandler;
import hk.hku.cecid.ebms.spa.listener.EbmsRequest;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.rest.RestRequest;
import hk.hku.cecid.piazza.commons.util.Generator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Sends an ebMS Ping on behalf of the admin console. The MSH routes it by
 * CPA: through the CPA's own Ping partnership if it has one, otherwise
 * through any enabled partnership of that CPA (see AgreementHandler). The
 * partner's MSH answers with a Pong, which shows up in Message History.
 */
class PingSender {

    static final String DEFAULT_FROM_PARTY_ID = "hermes-admin";

    static final String CREATED_VIA = "admin_console";

    private PingSender() {
    }

    /**
     * @return the ID of the Ping message submitted to the outbox.
     */
    static String send(String cpaId, String fromPartyId, String toPartyId,
            HttpServletRequest request) throws Exception {
        String messageId = Generator.generateMessageID();
        EbxmlMessage ebxmlMessage = new EbxmlMessage();
        MessageHeader msgHeader = ebxmlMessage.addMessageHeader();
        msgHeader.setCpaId(cpaId);
        msgHeader.setService(MessageClassifier.SERVICE);
        msgHeader.setAction(MessageClassifier.ACTION_PING);
        msgHeader.addFromPartyId(fromPartyId);
        msgHeader.addToPartyId(toPartyId);
        msgHeader.setConversationId(messageId);
        msgHeader.setMessageId(messageId);
        msgHeader.setTimestamp(EbmsUtility.getCurrentUTCDateTime());

        // OutboundMessageProcessor only accepts SOAP/WebServices/REST sources;
        // wrap the admin request the same way the REST send API does
        EbmsRequest ebmsRequest = new EbmsRequest(new RestRequest(request));
        ebmsRequest.setMessage(ebxmlMessage);
        MessageServiceHandler.getInstance().processOutboundMessage(ebmsRequest, null);

        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);
        MessageDVO createdMessage = (MessageDVO) messageDAO.createDVO();
        createdMessage.setMessageId(messageId);
        createdMessage.setMessageBox(MessageClassifier.MESSAGE_BOX_OUTBOX);
        if (messageDAO.findMessage(createdMessage)) {
            createdMessage.setCreatedVia(CREATED_VIA);
            messageDAO.persist(createdMessage);
        }
        return messageId;
    }

    static final String RESULT_OK = "ok";
    static final String RESULT_ERROR = "error";
    static final String RESULT_FAILED = "failed";
    static final String RESULT_WAITING = "waiting";

    /**
     * @return what has come back for the Ping so far: a Pong (ok), an ebMS
     *         Error (error), a delivery failure (failed) or nothing yet
     *         (waiting).
     */
    static String result(MessageDAO messageDAO, MessageDVO ping) throws DAOException {
        if (findReply(messageDAO, ping.getMessageId(), MessageClassifier.MESSAGE_TYPE_PONG) != null) {
            return RESULT_OK;
        }
        // checked before Error: a failed delivery also files a local Error
        // message against the Ping
        if (MessageClassifier.INTERNAL_STATUS_DELIVERY_FAILURE.equals(ping.getStatus())) {
            return RESULT_FAILED;
        }
        if (findReply(messageDAO, ping.getMessageId(), MessageClassifier.MESSAGE_TYPE_ERROR) != null) {
            return RESULT_ERROR;
        }
        return RESULT_WAITING;
    }

    static MessageDVO findReply(MessageDAO messageDAO, String pingMessageId,
            String messageType) throws DAOException {
        MessageDVO reply = (MessageDVO) messageDAO.createDVO();
        reply.setRefToMessageId(pingMessageId);
        reply.setMessageBox(MessageClassifier.MESSAGE_BOX_INBOX);
        reply.setMessageType(messageType);
        return messageDAO.findRefToMessage(reply) ? reply : null;
    }

    /**
     * Waits until every given Ping has an outcome or the timeout passes, so
     * the page can show the result right away. The outbox collector sends
     * the Pings in the background; this only polls the message table.
     *
     * @return Ping message ID -> result, in the order given.
     */
    static Map awaitResults(List pingMessageIds, long timeoutMillis)
            throws DAOException {
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);
        Map results = new LinkedHashMap();
        for (Iterator i = pingMessageIds.iterator(); i.hasNext();) {
            results.put(i.next(), RESULT_WAITING);
        }
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            boolean pending = false;
            for (Iterator i = results.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                if (!RESULT_WAITING.equals(entry.getValue())) {
                    continue;
                }
                MessageDVO ping = (MessageDVO) messageDAO.createDVO();
                ping.setMessageId((String) entry.getKey());
                ping.setMessageBox(MessageClassifier.MESSAGE_BOX_OUTBOX);
                if (messageDAO.findMessage(ping)) {
                    entry.setValue(result(messageDAO, ping));
                }
                pending |= RESULT_WAITING.equals(entry.getValue());
            }
            if (!pending || System.currentTimeMillis() >= deadline) {
                return results;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return results;
            }
        }
    }
}
