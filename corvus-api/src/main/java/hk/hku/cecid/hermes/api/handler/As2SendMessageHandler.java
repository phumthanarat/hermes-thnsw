package hk.hku.cecid.hermes.api.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hk.hku.cecid.edi.as2.AS2PlusProcessor;
import hk.hku.cecid.edi.as2.dao.MessageDAO;
import hk.hku.cecid.edi.as2.dao.MessageDVO;
import hk.hku.cecid.edi.as2.dao.PartnershipDAO;
import hk.hku.cecid.edi.as2.dao.PartnershipDVO;
import hk.hku.cecid.edi.as2.pkg.AS2Message;
import hk.hku.cecid.hermes.api.ErrorCode;
import hk.hku.cecid.hermes.api.listener.HermesAbstractApiListener;
import hk.hku.cecid.hermes.api.spa.ApiPlugin;
import hk.hku.cecid.piazza.commons.activation.ByteArrayDataSource;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.rest.RestRequest;

import org.apache.commons.codec.binary.Base64;


public class As2SendMessageHandler extends MessageHandler implements SendMessageHandler {

    public As2SendMessageHandler(HermesAbstractApiListener listener) {
        super(listener);
    }

    public Map<String, Object> getMessageStatus(String messageId) {
        ApiPlugin.core.log.debug("Parameters: id=" + messageId);

        try {
            MessageDAO msgDAO = (MessageDAO) AS2PlusProcessor.getInstance().getDAOFactory().createDAO(MessageDAO.class);
            MessageDVO message = (MessageDVO) msgDAO.createDVO();
            message.setMessageId(messageId);
            message.setMessageBox(MessageDVO.MSGBOX_OUT);
            message.setAs2From("%");
            message.setAs2To("%");
            message.setStatus("%");

            List messages = msgDAO.findMessagesByHistory(message, MAX_NUMBER, 0);
            if (messages.size() > 0) {
                String status = ((MessageDVO) messages.get(0)).getStatus();
                Map<String, Object> returnObj = new HashMap<String, Object>();
                returnObj.put("message_id", messageId);
                returnObj.put("status", status);
                return returnObj;
            }
            else {
                String errorMessage = "Message with such id not found";
                ApiPlugin.core.log.error(errorMessage);
                return listener.createError(ErrorCode.ERROR_DATA_NOT_FOUND, errorMessage);
            }
        }
        catch (DAOException e) {
            String errorMessage = "DAO exception";
            ApiPlugin.core.log.error(errorMessage, e);
            return listener.createError(ErrorCode.ERROR_READING_DATABASE, errorMessage);
        }
    }

    public Map<String, Object> sendMessage(Map<String, Object> inputDict, RestRequest sourceRequest) {
        Map<String, Object> errorObject = new HashMap<String, Object>();

        String as2From = listener.getStringFromInput(inputDict, "as2_from", errorObject);
        if (as2From == null) {
            return errorObject;
        }
        String as2To = listener.getStringFromInput(inputDict, "as2_to", errorObject);
        if (as2To == null) {
            return errorObject;
        }
        String type = listener.getStringFromInput(inputDict, "type", errorObject);
        if (type == null) {
            return errorObject;
        }

        List<byte[]> payloads = new ArrayList<byte[]>();
        if (inputDict.containsKey("payload")) {
            String payloadString = (String) inputDict.get("payload");
            try {
                payloads.add(Base64.decodeBase64(payloadString.getBytes()));
            } catch (Exception e) {
                String errorMessage = "Error parsing parameter: payload";
                ApiPlugin.core.log.error(errorMessage, e);
                return listener.createError(ErrorCode.ERROR_PARSING_REQUEST, errorMessage);
            }
        }
        else if (inputDict.containsKey("payloads")) {
            try {
                List<Object> payloadStrings = (List<Object>) inputDict.get("payloads");
                for (Object payloadObj : payloadStrings) {
                    Map<String,Object> payloadMap = (Map<String,Object>) payloadObj;
                    if (payloadMap.containsKey("payload")) {
                        String payloadString = (String) payloadMap.get("payload");
                        payloads.add(Base64.decodeBase64(payloadString.getBytes()));
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Error parsing parameter: payloads";
                ApiPlugin.core.log.error(errorMessage, e);
                return listener.createError(ErrorCode.ERROR_PARSING_REQUEST, errorMessage);
            }
        }

        ApiPlugin.core.log.debug("Parameters: as2_from=" + as2From + ", as2_to=" + as2To +
                                 ", type=" + type + ", number of payloads=" + payloads.size());

        List<String> messageIds = new ArrayList<String>();
        try {
            PartnershipDAO partnershipDAO = (PartnershipDAO) AS2PlusProcessor.getInstance().getDAOFactory().createDAO(PartnershipDAO.class);
            PartnershipDVO partnershipDVO = partnershipDAO.findByParty(as2From, as2To);
            if (partnershipDVO == null) {
                throw new DAOException("No partnership [" + as2From + ", " + as2To + "] is registered");
            }

            MessageDAO msgDAO = (MessageDAO) AS2PlusProcessor.getInstance().getDAOFactory().createDAO(MessageDAO.class);

            if (payloads.size() > 0) {
                for (byte[] payload : payloads) {
                    String messageId = AS2Message.generateID();
                    ByteArrayDataSource dataSource = new ByteArrayDataSource(payload, type);
                    AS2PlusProcessor.getInstance().getOutgoingMessageProcessor()
                            .storeOutgoingMessage(messageId, type, partnershipDVO, dataSource);
                    messageIds.add(messageId);

                    // Mark the message as having been submitted through the Web Service API,
                    // so it can be told apart from genuine AS2 wire traffic or admin console actions.
                    MessageDVO createdMessage = (MessageDVO) msgDAO.createDVO();
                    createdMessage.setMessageId(messageId);
                    createdMessage.setMessageBox(MessageDVO.MSGBOX_OUT);
                    if (msgDAO.retrieve(createdMessage)) {
                        createdMessage.setCreatedVia("webservice_api");
                        msgDAO.persist(createdMessage);
                    }
                }
            }
        }
        catch (DAOException e) {
            String errorMessage = "Error loading partnership";
            ApiPlugin.core.log.error(errorMessage, e);
            return listener.createError(ErrorCode.ERROR_READING_DATABASE, errorMessage);
        }
        catch (Exception e) {
            String errorMessage = "Error persisting payloads";
            ApiPlugin.core.log.error(errorMessage, e);
            return listener.createError(ErrorCode.ERROR_WRITING_MESSAGE, errorMessage);
        }

        Map<String, Object> returnObj = new HashMap<String, Object>();
        List<Object> messageIdObjs = new ArrayList<Object>();
        for (String messageId : messageIds) {
            Map<String, Object> dict = new HashMap<String, Object>();
            dict.put("id", messageId);
            messageIdObjs.add(dict);
        }

        returnObj.put("ids", messageIdObjs);
        return returnObj;
    }
}
