package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.CpaPartyDAO;
import hk.hku.cecid.ebms.spa.dao.CpaPartyDVO;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;

import java.util.List;

/**
 * The party IDs the admin pages suggest for a CPA, in order of preference:
 * those recorded for the CPA (from its CPA file on upload, or saved by an
 * admin), else those of the latest message exchanged under it (an outbox
 * message as is, an inbox one the other way round), else none.
 */
class CpaPartyIds {

    static final String SOURCE_CPA_UPLOAD = CpaPartyDVO.SOURCE_CPA_UPLOAD;
    static final String SOURCE_ADMIN = CpaPartyDVO.SOURCE_ADMIN;
    static final String SOURCE_MESSAGE = "message";
    static final String SOURCE_NONE = "";

    static final String DEFAULT_TYPE = "string";

    /** Our party ID(s) (From of an outbound message), comma separated, or "". */
    final String from;

    /** The type of each From party ID, comma separated. */
    final String fromType;

    /** The partner's party ID(s) (To of an outbound message), or "". */
    final String to;

    /** The type of each To party ID, comma separated. */
    final String toType;

    /** Where they came from: one of the SOURCE_ constants. */
    final String source;

    private CpaPartyIds(String from, String fromType, String to, String toType, String source) {
        this.from = from == null ? "" : from;
        this.to = to == null ? "" : to;
        this.fromType = typesFor(this.from, fromType);
        this.toType = typesFor(this.to, toType);
        this.source = source;
    }

    /** One type per ID, defaulting each missing type to "string". */
    private static String typesFor(String ids, String types) {
        if (ids.length() == 0) {
            return "";
        }
        String[] idList = ids.split(",", -1);
        String[] typeList = types == null ? new String[0] : types.split(",", -1);
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < idList.length; i++) {
            String type = i < typeList.length ? typeList[i].trim() : "";
            out.append(i == 0 ? "" : ",").append(type.length() > 0 ? type : DEFAULT_TYPE);
        }
        return out.toString();
    }

    /**
     * ebMS requires the PartyIds of one party to have distinct types, so
     * several IDs sharing a type (e.g. the "string" default) are rejected.
     * 
     * @return why the IDs can't be used, or null if they can.
     */
    static String checkTypes(String ids, String types) {
        String[] typeList = typesFor(ids, types).split(",");
        for (int i = 0; i < typeList.length; i++) {
            for (int j = i + 1; j < typeList.length; j++) {
                if (typeList[i].equals(typeList[j])) {
                    return "Party IDs " + ids + " would share the type '" + typeList[i]
                            + "'; ebMS requires a different type for each ID of a party,"
                            + " so upload the CPA to take its types, or use a single ID";
                }
            }
        }
        return null;
    }

    static CpaPartyIds suggest(String cpaId) throws DAOException {
        CpaPartyDVO recorded = recorded(cpaId);
        if (recorded != null) {
            return new CpaPartyIds(recorded.getFromPartyId(), recorded.getFromPartyType(),
                    recorded.getToPartyId(), recorded.getToPartyType(), recorded.getSource());
        }

        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);
        MessageDVO outbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_OUTBOX, "%");
        if (outbound != null && outbound.getFromPartyId() != null) {
            return new CpaPartyIds(outbound.getFromPartyId(), outbound.getFromPartyRole(),
                    outbound.getToPartyId(), outbound.getToPartyRole(), SOURCE_MESSAGE);
        }
        MessageDVO inbound = latestMessage(messageDAO, cpaId,
                MessageClassifier.MESSAGE_BOX_INBOX, "%");
        if (inbound != null && inbound.getToPartyId() != null) {
            return new CpaPartyIds(inbound.getToPartyId(), inbound.getToPartyRole(),
                    inbound.getFromPartyId(), inbound.getFromPartyRole(), SOURCE_MESSAGE);
        }
        return new CpaPartyIds("", "", "", "", SOURCE_NONE);
    }

    /** Records the party IDs an admin entered for the CPA. */
    static void save(String cpaId, String from, String to) throws DAOException {
        CpaPartyDVO existing = recorded(cpaId);
        CpaPartyDAO dao = (CpaPartyDAO) EbmsProcessor.core.dao.createDAO(CpaPartyDAO.class);
        CpaPartyDVO parties = (CpaPartyDVO) dao.createDVO();
        parties.setCpaId(cpaId);
        parties.setFromPartyId(from);
        parties.setToPartyId(to);
        // keep the CPA's types while the IDs they describe are unchanged
        parties.setFromPartyType(existing != null && from.equals(existing.getFromPartyId())
                ? existing.getFromPartyType() : typesFor(from, null));
        parties.setToPartyType(existing != null && to.equals(existing.getToPartyId())
                ? existing.getToPartyType() : typesFor(to, null));
        parties.setSource(SOURCE_ADMIN);
        String problem = checkTypes(from, parties.getFromPartyType());
        if (problem == null) {
            problem = checkTypes(to, parties.getToPartyType());
        }
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        dao.save(parties);
    }

    private static CpaPartyDVO recorded(String cpaId) {
        try {
            CpaPartyDAO dao = (CpaPartyDAO) EbmsProcessor.core.dao.createDAO(CpaPartyDAO.class);
            return dao.findByCpaId(cpaId);
        } catch (DAOException e) {
            // e.g. a database created before the cpa_party table existed
            EbmsProcessor.core.log.warn("Unable to read the party IDs of CPA " + cpaId
                    + ": " + e.getMessage());
            return null;
        }
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
