package hk.hku.cecid.piazza.corvus.core.main.admin.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * CaIssuedCertDAO is the data access object for certificates issued by this
 * gateway's own internal CA (see {@link
 * hk.hku.cecid.piazza.corvus.core.main.admin.ca.CertificateAuthority}).
 */
public interface CaIssuedCertDAO extends DAO {

    public CaIssuedCertDVO findBySerialNumber(String serialNumber) throws DAOException;

    public List findAllCerts() throws DAOException;

    public List findRevokedCerts() throws DAOException;
}
