package hk.hku.cecid.ebms.spa.dao;

import org.junit.Assert;
import org.junit.Test;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.test.DAOTest;

public class CpaPartyDataSourceDAOTest extends DAOTest<CpaPartyDataSourceDAO> {

	@Override
	public String getTableName() {
		return "cpa_party";
	}

	@Test
	public void testFindByCpaId() throws DAOException {
		CpaPartyDVO found = getTestingTarget().findByCpaId("cpa1");
		Assert.assertNotNull(found);
		Assert.assertEquals("THNSW", found.getFromPartyId());
		Assert.assertEquals("THTSM00", found.getToPartyId());
		Assert.assertEquals(CpaPartyDVO.SOURCE_CPA_UPLOAD, found.getSource());
		Assert.assertNull(getTestingTarget().findByCpaId("no-such-cpa"));
	}

	@Test
	public void testSaveInsertsThenReplaces() throws DAOException {
		CpaPartyDataSourceDAO dao = getTestingTarget();
		CpaPartyDVO dvo = (CpaPartyDVO) dao.createDVO();
		dvo.setCpaId("cpa2");
		dvo.setFromPartyId("A");
		dvo.setToPartyId("B,C");
		dvo.setToPartyType("string,urn");
		dvo.setSource(CpaPartyDVO.SOURCE_ADMIN);
		dao.save(dvo);
		Assert.assertEquals("B,C", dao.findByCpaId("cpa2").getToPartyId());

		CpaPartyDVO update = (CpaPartyDVO) dao.createDVO();
		update.setCpaId("cpa1");
		update.setFromPartyId("NEW");
		update.setToPartyId("THTSM00");
		update.setSource(CpaPartyDVO.SOURCE_ADMIN);
		dao.save(update);
		CpaPartyDVO saved = dao.findByCpaId("cpa1");
		Assert.assertEquals("NEW", saved.getFromPartyId());
		Assert.assertEquals(CpaPartyDVO.SOURCE_ADMIN, saved.getSource());
	}
}
