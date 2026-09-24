package hk.hku.cecid.ebms.admin.listener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import hk.hku.cecid.ebms.spa.dao.MessageDAO;

import junit.framework.TestCase;
import hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptorTest;;

public class MessageHistoryPageletAdaptorTest extends TestCase {

		private MessageHistoryPageletAdaptor adaptor = new MessageHistoryPageletAdaptor();
		
		// Invoked for setup.
		public void setUp() throws Exception {
			System.out.println();
			System.out.println("---------" + this.getName() + " Start -------");
		}
		
		// Invoked for finalized.
		public void tearDown() throws Exception {
			System.out.println("---------" + this.getName() + " End   -------");
		}	
		
		public void testCheckEmptyAndReturnNull() throws Exception {
			Method m = Class.forName("hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptor").
				getDeclaredMethod("checkEmptyAndReturnNull", new Class[] {String.class});
			m.setAccessible(true);
			TestCase.assertEquals("abcdefg", (String)m.invoke(adaptor, new Object[] {"abcdefg"}));
			TestCase.assertEquals(null, (String)m.invoke(adaptor, new Object[] {""}));
		}	
		
		public void testCheckNullAndReturnEmpty() throws Exception {
			Method m = Class.forName("hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptor").
				getDeclaredMethod("checkNullAndReturnEmpty", new Class[] {String.class});
			m.setAccessible(true);
			TestCase.assertEquals("abcdefg", (String)m.invoke(adaptor, new Object[] {"abcdefg"}));
			TestCase.assertEquals("", (String)m.invoke(adaptor, new Object[] {null}));			
		}
		
		public void testCheckStarAndConvertToPercent() throws Exception {
			Method m = Class.forName("hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptor").
			getDeclaredMethod("checkStarAndConvertToPercent", new Class[] {String.class});
			m.setAccessible(true);
			TestCase.assertEquals("abcdefg", (String)m.invoke(adaptor, new Object[] {"abcdefg"}));
			TestCase.assertEquals("%", (String)m.invoke(adaptor, new Object[] {null}));
			TestCase.assertEquals("%", (String)m.invoke(adaptor, new Object[] {""}));
			TestCase.assertEquals("%", (String)m.invoke(adaptor, new Object[] {"*"}));
			TestCase.assertEquals("abc\\_def", (String)m.invoke(adaptor, new Object[] {"abc_def"}));
			TestCase.assertEquals("abc\\%def", (String)m.invoke(adaptor, new Object[] {"abc%def"}));
			TestCase.assertEquals("abc%def", (String)m.invoke(adaptor, new Object[] {"abc*def"}));
			TestCase.assertEquals("\\%%\\_\\_%\\%", (String)m.invoke(adaptor, new Object[] {"%*__*%"}));
		}

		/** A MessageDAO that records the page bounds findMessagesByHistory is given. */
		private MessageDAO recordingDAO(final List calls) {
			return (MessageDAO) Proxy.newProxyInstance(MessageDAO.class.getClassLoader(),
				new Class[] {MessageDAO.class}, new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) {
						if (method.getName().equals("findMessagesByHistory") && args.length == 5) {
							calls.add(args[3] + "," + args[4]);
							return new ArrayList();
						}
						throw new UnsupportedOperationException(method.getName());
					}
				});
		}

		public void testFindMessagesPassesPageSizeAndOffset() throws Exception {
			List calls = new ArrayList();
			adaptor.findMessages(recordingDAO(calls), null, null, null, 20, 40);
			TestCase.assertEquals("20,40", calls.get(0));
		}

		/** Oracle's query bounds the page with "rownum <= ?", the page's last row. */
		public void testOracleFindMessagesPassesLastRowOfPage() throws Exception {
			List calls = new ArrayList();
			new MessageHistoryOraclePageletAdaptor().findMessages(recordingDAO(calls), null, null, null, 20, 40);
			TestCase.assertEquals("60,40", calls.get(0));
		}

		public void testIsInProgress() throws Exception {
			Method m = MessageHistoryPageletAdaptor.class.getDeclaredMethod("isInProgress", new Class[] {String.class});
			m.setAccessible(true);
			TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new Object[] {"PD"}));
			TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new Object[] {"PR"}));
			TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new Object[] {"DL"}));
			TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new Object[] {"PS"}));
			TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new Object[] {"DF"}));
			TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new Object[] {null}));
		}

		public void testParseDateTime() throws Exception {
			Method m = MessageHistoryPageletAdaptor.class.getDeclaredMethod("parseDateTime", new Class[] {String.class});
			m.setAccessible(true);
			TestCase.assertEquals(Timestamp.valueOf("2026-09-24 10:35:00"),
				m.invoke(adaptor, new Object[] {"2026-09-24T10:35"}));
			TestCase.assertNull(m.invoke(adaptor, new Object[] {null}));
			TestCase.assertNull(m.invoke(adaptor, new Object[] {""}));
			TestCase.assertNull(m.invoke(adaptor, new Object[] {"garbage"}));
			// not lenient: no rolling 13th month into next year
			TestCase.assertNull(m.invoke(adaptor, new Object[] {"2026-13-01T00:00"}));
		}
}
