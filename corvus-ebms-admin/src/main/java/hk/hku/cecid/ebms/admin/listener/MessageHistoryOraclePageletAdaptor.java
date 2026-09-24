package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;

import java.sql.Timestamp;
import java.util.List;


/**
 * @author Donahue Sze
 *  
 */
public class MessageHistoryOraclePageletAdaptor extends MessageHistoryPageletAdaptor {
	/**
	 * Oracle's history query pages by ROWNUM: "rownum <= ?" takes the last
	 * row of the page, so pass offset + page size rather than the page size.
	 */
	protected List findMessages(MessageDAO messageDAO, MessageDVO criteria,
			Timestamp fromTime, Timestamp toTime, int numberOfMessages,
			int offset) throws DAOException {
		return messageDAO.findMessagesByHistory(criteria, fromTime, toTime,
				numberOfMessages + offset, offset);
	}
}