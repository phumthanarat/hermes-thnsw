package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

public class CpaPartyDataSourceDAO extends DataSourceDAO implements CpaPartyDAO {

    public DVO createDVO() {
        return new CpaPartyDataSourceDVO();
    }

    public CpaPartyDVO findByCpaId(String cpaId) throws DAOException {
        CpaPartyDataSourceDVO dvo = (CpaPartyDataSourceDVO) createDVO();
        dvo.setCpaId(cpaId);
        return super.retrieve(dvo) ? dvo : null;
    }

    public void save(CpaPartyDVO data) throws DAOException {
        CpaPartyDataSourceDVO existing = (CpaPartyDataSourceDVO) createDVO();
        existing.setCpaId(data.getCpaId());
        if (super.retrieve(existing)) {
            super.persist((CpaPartyDataSourceDVO) data);
        } else {
            super.create((CpaPartyDataSourceDVO) data);
        }
    }
}
