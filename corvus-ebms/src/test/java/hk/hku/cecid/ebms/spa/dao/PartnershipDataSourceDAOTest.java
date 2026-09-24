package hk.hku.cecid.ebms.spa.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import hk.hku.cecid.ebms.spa.handler.MessageClassifier;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.test.DAOTest;

public class PartnershipDataSourceDAOTest extends DAOTest<PartnershipDataSourceDAO> {

	@Override
	public String getTableName() {
		return "partnership";
	}

	private PartnershipDVO criteria(String cpaId, String service, String action) {
		PartnershipDVO dvo = (PartnershipDVO) getTestingTarget().createDVO();
		dvo.setCpaId(cpaId);
		dvo.setService(service);
		dvo.setAction(action);
		return dvo;
	}

	private PartnershipDVO ping(String cpaId) {
		return criteria(cpaId, MessageClassifier.SERVICE, MessageClassifier.ACTION_PING);
	}

	@Test
	public void testFindPartnershipsByCpaIdSkipsDisabledAndOrdersById() throws DAOException {
		List found = getTestingTarget().findPartnershipsByCpaId("cpa1");
		Assert.assertEquals(2, found.size());
		Assert.assertEquals("b-cpa1-order", ((PartnershipDVO) found.get(0)).getPartnershipId());
		Assert.assertEquals("c-cpa1-invoice", ((PartnershipDVO) found.get(1)).getPartnershipId());
	}

	@Test
	public void testBusinessMessageNeedsExactMatch() throws DAOException {
		PartnershipDVO dvo = criteria("cpa1", "svc:invoice", "Invoice");
		Assert.assertTrue(getTestingTarget().findPartnershipForMessage(dvo));
		Assert.assertEquals("c-cpa1-invoice", dvo.getPartnershipId());

		// no fallback for anything but the ebMS service
		Assert.assertFalse(getTestingTarget().findPartnershipForMessage(
				criteria("cpa1", "svc:unknown", "Unknown")));
	}

	@Test
	public void testPingWithoutPingPartnershipBorrowsFirstEnabledOne() throws DAOException {
		PartnershipDVO dvo = ping("cpa1");
		Assert.assertTrue(getTestingTarget().findPartnershipForMessage(dvo));
		// a-cpa1-disabled sorts first but is disabled
		Assert.assertEquals("b-cpa1-order", dvo.getPartnershipId());
		Assert.assertEquals("http://partner1/", dvo.getTransportEndpoint());
		Assert.assertEquals("mshSignalsOnly", dvo.getSyncReplyMode());
	}

	@Test
	public void testPingPrefersTheCpasOwnPingPartnership() throws DAOException {
		PartnershipDVO dvo = ping("cpa2");
		Assert.assertTrue(getTestingTarget().findPartnershipForMessage(dvo));
		Assert.assertEquals("z-cpa2-ping", dvo.getPartnershipId());
	}

	@Test
	public void testPingFindsNothingWithoutAnEnabledPartnership() throws DAOException {
		Assert.assertFalse(getTestingTarget().findPartnershipForMessage(ping("cpa3")));
		Assert.assertFalse(getTestingTarget().findPartnershipForMessage(ping("no-such-cpa")));
	}

	@Test
	public void testExactLookupIsUnchanged() throws DAOException {
		// duplicate checks (e.g. the partnership REST API) rely on this
		Assert.assertFalse(getTestingTarget().findPartnershipByCPA(ping("cpa1")));
	}
}
