package hk.hku.cecid.piazza.corvus.core.main.admin.dao.ds;

import java.util.List;

import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDAO;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * CaIssuedCertDSDAO is the data source implementation of CaIssuedCertDAO.
 */
public class CaIssuedCertDSDAO extends DataSourceDAO implements CaIssuedCertDAO {

    public DVO createDVO() {
        return new CaIssuedCertDSDVO();
    }

    public CaIssuedCertDVO findBySerialNumber(String serialNumber) throws DAOException {
        CaIssuedCertDVO dvo = (CaIssuedCertDVO) createDVO();
        dvo.setSerialNumber(serialNumber);
        if (super.retrieve(dvo)) {
            return dvo;
        }
        return null;
    }

    public List findAllCerts() throws DAOException {
        return super.find("find_all_certs", null);
    }

    public List findRevokedCerts() throws DAOException {
        return super.find("find_revoked_certs", null);
    }
}
