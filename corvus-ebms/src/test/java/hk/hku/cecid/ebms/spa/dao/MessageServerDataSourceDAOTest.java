package hk.hku.cecid.ebms.spa.dao;

import org.junit.Assert;
import org.junit.Test;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.test.DAOTest;

public class MessageServerDataSourceDAOTest extends DAOTest<MessageServerDataSourceDAO> {

	@Override
	public String getTableName() {
		return "message";
	}

	private MessageDAO messageDAO() throws DAOException {
		return (MessageDAO) daoFactory.createDAO(MessageDAO.class);
	}

	private boolean messageExists(String id, String box) throws DAOException {
		MessageDVO dvo = (MessageDVO) messageDAO().createDVO();
		dvo.setMessageId(id);
		dvo.setMessageBox(box);
		return messageDAO().findMessage(dvo);
	}

	private boolean repositoryExists(String id, String box) throws DAOException {
		RepositoryDAO dao = (RepositoryDAO) daoFactory.createDAO(RepositoryDAO.class);
		RepositoryDVO dvo = (RepositoryDVO) dao.createDVO();
		dvo.setMessageId(id);
		dvo.setMessageBox(box);
		return dao.findRepository(dvo);
	}

	private boolean outboxExists(String id) throws DAOException {
		OutboxDAO dao = (OutboxDAO) daoFactory.createDAO(OutboxDAO.class);
		OutboxDVO dvo = (OutboxDVO) dao.createDVO();
		dvo.setMessageId(id);
		return dao.findOutbox(dvo);
	}

	private boolean inboxExists(String id) throws DAOException {
		InboxDAO dao = (InboxDAO) daoFactory.createDAO(InboxDAO.class);
		InboxDVO dvo = (InboxDVO) dao.createDVO();
		dvo.setMessageId(id);
		return dao.findInbox(dvo);
	}

	private void delete(String id, String box) throws DAOException {
		MessageDVO dvo = (MessageDVO) messageDAO().createDVO();
		dvo.setMessageId(id);
		dvo.setMessageBox(box);
		getTestingTarget().deleteMessage(dvo);
	}

	@Test
	public void testDeleteOutboxMessageRemovesAllItsRows() throws DAOException {
		Assert.assertTrue(messageExists("del-out@test", "outbox"));
		Assert.assertTrue(repositoryExists("del-out@test", "outbox"));
		Assert.assertTrue(outboxExists("del-out@test"));

		delete("del-out@test", "outbox");

		Assert.assertFalse(messageExists("del-out@test", "outbox"));
		Assert.assertFalse(repositoryExists("del-out@test", "outbox"));
		Assert.assertFalse(outboxExists("del-out@test"));
		// other messages are untouched
		Assert.assertTrue(messageExists("keep@test", "outbox"));
		Assert.assertTrue(repositoryExists("keep@test", "outbox"));
		Assert.assertTrue(outboxExists("keep@test"));
	}

	@Test
	public void testDeleteInboxMessageRemovesAllItsRows() throws DAOException {
		Assert.assertTrue(inboxExists("del-in@test"));

		delete("del-in@test", "inbox");

		Assert.assertFalse(messageExists("del-in@test", "inbox"));
		Assert.assertFalse(repositoryExists("del-in@test", "inbox"));
		Assert.assertFalse(inboxExists("del-in@test"));
	}
}
