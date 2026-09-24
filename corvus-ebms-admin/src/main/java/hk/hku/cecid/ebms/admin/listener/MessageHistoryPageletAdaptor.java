package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.dao.MessageServerDAO;
import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * @author Donahue Sze
 *  
 */
public class MessageHistoryPageletAdaptor extends AdminPageletAdaptor {

    /*
     * (non-Javadoc)
     * 
     * @see hk.hku.cecid.piazza.commons.pagelet.xslt.BorderLayoutPageletAdaptor#getCenterSource(javax.servlet.http.HttpServletRequest)
     */
    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = null;

        try {
            if ("post".equalsIgnoreCase(request.getMethod())
                    && "delete".equals(request.getParameter("request_action"))) {
                deleteMessages(request);
            }
            // construct updated delivery channels property tree
            dom = getMessageHistory(request);
        } catch (DAOException e) {
            EbmsProcessor.core.log.debug(
                    "Unable to process the message search page request", e);
            throw new RuntimeException(
                    "Unable to process the message search page request", e);
        }

        return dom.getSource();
    }

    /**
     * @param request
     * @return
     * @throws DAOException
     */
    private PropertyTree getMessageHistory(HttpServletRequest request)
            throws DAOException {

        // construct the dom tree
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/message_history", "");

        int numberOfMessagesInt = 20; // default value
        int offsetInt = 0;
        boolean isDetail = false;
        boolean isTime = false;
        int displayLastInt = 0;
        String fromTimeParam = null;
        String toTimeParam = null;

        // get the input parameters
        Iterator messageIterator = null;

        if (request.getParameter("original_message_id") != null) {
            String originalMessageId = request
                    .getParameter("original_message_id");
            String originalMessageBox = request
                    .getParameter("original_message_box");
            String refToMessageType = request
                    .getParameter("ref_to_message_type");
            String refToMessageBox = originalMessageBox
                    .equals(MessageClassifier.MESSAGE_BOX_INBOX) ? MessageClassifier.MESSAGE_BOX_OUTBOX
                    : MessageClassifier.MESSAGE_BOX_INBOX;

            isDetail = true;

            // search the corresponding messages
            MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                    .createDAO(MessageDAO.class);
            MessageDVO messageDVO = (MessageDVO) messageDAO.createDVO();
            messageDVO.setRefToMessageId(originalMessageId);
            messageDVO.setMessageBox(refToMessageBox);
            messageDVO.setMessageType(refToMessageType);

            List messageList = new ArrayList();
            if (messageDAO.findRefToMessage(messageDVO)) {
                messageList.add(messageDVO);
            }

            messageIterator = messageList.iterator();

            dom.setProperty("total_no_of_messages", String.valueOf(messageList
                    .size()));
        } else {
            // text field
            String messageId = checkStarAndConvertToPercent(request
                    .getParameter("message_id"));
            String cpaId = checkStarAndConvertToPercent(request
                    .getParameter("cpa_id"));
            String service = checkStarAndConvertToPercent(request
                    .getParameter("service"));
            String action = checkStarAndConvertToPercent(request
                    .getParameter("action"));
            String convId = checkStarAndConvertToPercent(request
                    .getParameter("conv_id"));     
            
            String primalMessageId = checkEmptyAndReturnNull(request
            		.getParameter("primal_message_id"));
            // radio button and menu
            String messageBox = checkEmptyAndReturnNull(request
                    .getParameter("message_box"));
            String status = checkEmptyAndReturnNull(request
                    .getParameter("status"));
            // the page shows every message type unless one is picked
            String messageType = checkEmptyAndReturnNull(request
                    .getParameter("message_type"));
            if (messageType == null || "all".equals(messageType)) {
                messageType = "%";
            }

            //get the message_time value
            String displayLast = request.getParameter("message_time");
            if(displayLast != null){
            	if(!(displayLast.equals(""))){
            		displayLastInt = Integer.valueOf(displayLast).intValue();
            		// 0 is what the page echoes back for "All"
            		isTime = displayLastInt > 0;
            	}
            }

            // the From/To pickers send "yyyy-MM-ddTHH:mm"; a value that
            // doesn't parse is ignored rather than failing the search
            Timestamp fromTime = parseDateTime(request.getParameter("from_time"));
            Timestamp toTime = parseDateTime(request.getParameter("to_time"));
            if (fromTime != null) {
                fromTimeParam = request.getParameter("from_time");
            }
            if (toTime != null) {
                toTimeParam = request.getParameter("to_time");
                // the picker has minute precision, so "To" includes that
                // whole minute
                toTime = new Timestamp(toTime.getTime() + 60 * 1000);
            }
            // "Messages for the Last" is just another lower bound, applied
            // in the query so the total count and paging agree with it
            if (isTime) {
                GregorianCalendar calendar = new GregorianCalendar();
                calendar.add(GregorianCalendar.MONTH, -displayLastInt);
                Timestamp cutOff = new Timestamp(calendar.getTimeInMillis());
                if (fromTime == null || cutOff.after(fromTime)) {
                    fromTime = cutOff;
                }
            }

            String numOfMessages = request.getParameter("num_of_messages");
            if (numOfMessages != null) {
                numberOfMessagesInt = Integer.valueOf(numOfMessages).intValue();
            }

            String offset = request.getParameter("offset");
            if (offset != null) {
                offsetInt = Integer.valueOf(offset).intValue();
            }

            String isDetailStr = request.getParameter("is_detail");
            if (isDetailStr != null) {
                if (isDetailStr.equalsIgnoreCase("true")) {
                    isDetail = true;
                }
            }

            // search the corresponding messages
            MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                    .createDAO(MessageDAO.class);
            MessageDVO messageDVO = (MessageDVO) messageDAO.createDVO();

            messageDVO.setMessageId(messageId);
            messageDVO.setCpaId(cpaId);
            messageDVO.setService(service);
            messageDVO.setAction(action);
            messageDVO.setConvId(convId);
            messageDVO.setMessageBox(messageBox);
            messageDVO.setStatus(status);
            messageDVO.setMessageType(messageType);
            messageDVO.setPrimalMessageId(primalMessageId);

			messageIterator = findMessages(messageDAO, messageDVO,
					fromTime, toTime, numberOfMessagesInt, offsetInt).iterator();
            dom.setProperty("total_no_of_messages", String.valueOf(messageDAO
                    .findNumberOfMessagesByHistory(messageDVO, fromTime, toTime)));
        }

        // pass the search criteria
        dom.setProperty("search_criteria/message_id", request
                .getParameter("message_id"));
        dom.setProperty("search_criteria/message_box", request
                .getParameter("message_box"));
        dom.setProperty("search_criteria/cpa_id", request
                .getParameter("cpa_id"));
        dom.setProperty("search_criteria/service", request
                .getParameter("service"));
        dom.setProperty("search_criteria/action", request
                .getParameter("action"));
        dom.setProperty("search_criteria/conv_id", request
                .getParameter("conv_id"));
        dom.setProperty("search_criteria/status", request
                .getParameter("status"));
        dom.setProperty("search_criteria/message_type", request
                .getParameter("message_type"));
        dom.setProperty("search_criteria/num_of_messages", String
                .valueOf(numberOfMessagesInt));
        dom.setProperty("search_criteria/message_time",String.valueOf(displayLastInt));
        dom.setProperty("search_criteria/from_time", checkNullAndReturnEmpty(fromTimeParam));
        dom.setProperty("search_criteria/to_time", checkNullAndReturnEmpty(toTimeParam));
        dom.setProperty("search_criteria/offset", String.valueOf(offsetInt));
        dom.setProperty("search_criteria/is_detail", String.valueOf(isDetail));
        dom.setProperty("search_criteria/primal_message_id", request
                .getParameter("primal_message_id"));

        for (int pi = 1; messageIterator.hasNext(); pi++) {
            MessageDVO returnData = (MessageDVO) messageIterator.next();

            dom.setProperty("message[" + pi + "]/message_id",
                    checkNullAndReturnEmpty(returnData.getMessageId()));
            dom.setProperty("message[" + pi + "]/message_box",
                    checkNullAndReturnEmpty(returnData.getMessageBox()));
            dom.setProperty("message[" + pi + "]/ack_requested",
                    checkNullAndReturnEmpty(returnData.getAckRequested()));
            dom.setProperty("message[" + pi + "]/ref_to_message_id",
                    checkNullAndReturnEmpty(returnData.getRefToMessageId()));
            dom.setProperty("message[" + pi + "]/message_type",
                    checkNullAndReturnEmpty(returnData.getMessageType()));
            dom.setProperty("message[" + pi + "]/cpa_id",
                    checkNullAndReturnEmpty(returnData.getCpaId()));
            dom.setProperty("message[" + pi + "]/service",
                    checkNullAndReturnEmpty(returnData.getService()));
            dom.setProperty("message[" + pi + "]/action",
                    checkNullAndReturnEmpty(returnData.getAction()));
            dom.setProperty("message[" + pi + "]/conv_id",
                    checkNullAndReturnEmpty(returnData.getConvId()));
            dom.setProperty("message[" + pi + "]/time_stamp", returnData
                    .getTimeStamp().toString());
            dom.setProperty("message[" + pi + "]/primal_message_id", 
            		checkNullAndReturnEmpty(returnData.getPrimalMessageId()));
            dom.setProperty("message[" + pi + "]/status",
                    checkNullAndReturnEmpty(returnData.getStatus()));
            dom.setProperty("message[" + pi + "]/has_resend_as_new",
                    checkNullAndReturnEmpty(returnData.getHasResendAsNew()));
            dom.setProperty("message[" + pi + "]/created_via",
                    checkNullAndReturnEmpty(returnData.getCreatedVia()));

            if (isDetail) {
                dom.setProperty("message[" + pi + "]/from_party_id",
                        checkNullAndReturnEmpty(returnData.getFromPartyId()));
                dom.setProperty("message[" + pi + "]/from_party_type",
                        checkNullAndReturnEmpty(returnData.getFromPartyRole()));
                dom.setProperty("message[" + pi + "]/to_party_id",
                        checkNullAndReturnEmpty(returnData.getToPartyId()));
                dom.setProperty("message[" + pi + "]/to_party_type",
                        checkNullAndReturnEmpty(returnData.getToPartyRole()));
                dom.setProperty("message[" + pi + "]/ack_sign_requested",
                        checkNullAndReturnEmpty(returnData
                                .getAckSignRequested()));
                if (returnData.getSequenceStatus() != -1) {
                    dom.setProperty("message[" + pi + "]/sequence_group",
                            String.valueOf(returnData.getSequenceGroup()));
                    dom.setProperty("message[" + pi + "]/sequence_no", String
                            .valueOf(returnData.getSequenceNo()));
                    dom.setProperty("message[" + pi + "]/sequence_status",
                            String.valueOf(returnData.getSequenceStatus()));
                }
                dom.setProperty("message[" + pi + "]/status_description",
                        checkNullAndReturnEmpty(returnData
                                .getStatusDescription()));
            }
        }

        return dom;
    }

    private String checkEmptyAndReturnNull(String parameter) {
        if (parameter == null || parameter.equals("")) {
            return null;
        }
        return parameter;
    }

    private String checkNullAndReturnEmpty(String parameter) {
        if (parameter == null) {
            return new String("");
        }
        return parameter;
    }

    /**
     * @param parameter
     * @return
     */
    private String checkStarAndConvertToPercent(String parameter) {
    	if (parameter == null || parameter.equals("")) {
            return "%";
        }
        return parameter.replace("_", "\\_").replace("%", "\\%").replace('*', '%');
    }

    /**
     * Deletes the messages ticked on the page, each posted as a "delete_key"
     * of "message ID|message box", with their repository content and
     * inbox/outbox entries.
     */
    private void deleteMessages(HttpServletRequest request) {
        String[] keys = request.getParameterValues("delete_key");
        if (keys == null || keys.length == 0) {
            request.setAttribute(ATTR_MESSAGE, "No message selected");
            return;
        }
        MessageServerDAO messageServerDAO;
        MessageDAO messageDAO;
        try {
            messageServerDAO = (MessageServerDAO) EbmsProcessor.core.dao
                    .createDAO(MessageServerDAO.class);
            messageDAO = (MessageDAO) EbmsProcessor.core.dao
                    .createDAO(MessageDAO.class);
        } catch (DAOException e) {
            throw new RuntimeException("Unable to delete messages", e);
        }

        int deleted = 0;
        StringBuffer failures = new StringBuffer();
        StringBuffer inProgress = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            int separator = keys[i].lastIndexOf('|');
            if (separator <= 0) {
                continue;
            }
            MessageDVO message = (MessageDVO) messageDAO.createDVO();
            message.setMessageId(keys[i].substring(0, separator));
            message.setMessageBox(keys[i].substring(separator + 1));
            try {
                // a message the MSH is still sending or processing would be
                // left half-handled; it can be deleted once it settles
                if (messageDAO.findMessage(message) && isInProgress(message.getStatus())) {
                    inProgress.append(inProgress.length() == 0 ? "" : "; ")
                            .append(message.getMessageId());
                    continue;
                }
                messageServerDAO.deleteMessage(message);
                EbmsProcessor.core.log.info("Deleted message from the admin console: "
                        + message.getMessageId() + " (" + message.getMessageBox() + ")");
                deleted++;
            } catch (DAOException e) {
                EbmsProcessor.core.log.error("Unable to delete message: "
                        + message.getMessageId(), e);
                failures.append(failures.length() == 0 ? "" : "; ")
                        .append(message.getMessageId());
            }
        }
        String result = "Deleted " + deleted + " message(s)";
        if (inProgress.length() > 0) {
            result += ". Not deleted, still pending/processing: " + inProgress;
        }
        if (failures.length() > 0) {
            result += ". Unable to delete: " + failures;
        }
        request.setAttribute(ATTR_MESSAGE, result);
    }

    /**
     * Finds one page of the search result. The Oracle adaptor overrides this
     * because its query bounds the page by row numbers, not LIMIT/OFFSET.
     */
    protected List findMessages(MessageDAO messageDAO, MessageDVO criteria,
            Timestamp fromTime, Timestamp toTime, int numberOfMessages,
            int offset) throws DAOException {
        return messageDAO.findMessagesByHistory(criteria, fromTime, toTime,
                numberOfMessages, offset);
    }

    private static boolean isInProgress(String status) {
        return MessageClassifier.INTERNAL_STATUS_PENDING.equals(status)
                || MessageClassifier.INTERNAL_STATUS_PROCESSING.equals(status);
    }

    private Timestamp parseDateTime(String parameter) {
        if (parameter == null || parameter.trim().equals("")) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        format.setLenient(false);
        try {
            return new Timestamp(format.parse(parameter.trim()).getTime());
        } catch (ParseException e) {
            return null;
        }
    }
}