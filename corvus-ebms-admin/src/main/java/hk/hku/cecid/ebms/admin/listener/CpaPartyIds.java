package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;

import java.util.List;

/**
 * Hermes keeps no party IDs per CPA, so the admin pages suggest them from
 * the latest message exchanged under the CPA: an outbox message as is, or an
 * inbox message the other way round.
 */
class CpaPartyIds {

    /** Our party ID (From of an outbound message), or "" if unknown. */
    final String from;

    /** The partner's party ID (To of an outbound message), or "" if unknown. */
    final String to;

    private CpaPartyIds(String from, String to) {
        this.from = from == null ? "" : from;
        this.to = to == null ? "" : to;
    }

    static CpaPartyIds suggest(String cpaId) throws DAOException {
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);
        MessageDVO outbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_OUTBOX, "%");
        if (outbound != null && outbound.getFromPartyId() != null) {
            return new CpaPartyIds(outbound.getFromPartyId(), outbound.getToPartyId());
        }
        MessageDVO inbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_INBOX, "%");
        if (inbound != null && inbound.getToPartyId() != null) {
            return new CpaPartyIds(inbound.getToPartyId(), inbound.getFromPartyId());
        }
        return new CpaPartyIds("", "");
    }

    static MessageDVO latestMessage(MessageDAO messageDAO, String cpaId,
            String messageBox, String messageType) throws DAOException {
        MessageDVO criteria = (MessageDVO) messageDAO.createDVO();
        // cpa_id is matched with LIKE, so its wildcards must be literal
        criteria.setCpaId(cpaId.replace("_", "\\_").replace("%", "\\%"));
        criteria.setMessageBox(messageBox);
        criteria.setMessageType(messageType);
        List found = messageDAO.findMessagesByHistory(criteria, 1, 0);
        return found.isEmpty() ? null : (MessageDVO) found.get(0);
    }
}
