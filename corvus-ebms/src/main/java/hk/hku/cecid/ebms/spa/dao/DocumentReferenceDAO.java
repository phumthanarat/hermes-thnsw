package hk.hku.cecid.ebms.spa.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * DocumentReferenceDAO is the data access object for reference files (XSD/
 * WSDL/sample XML) attached to a document type in the Document Catalog
 * admin page, for admins/developers to view or download -- kept purely for
 * reference; not used to validate any actual message traffic.
 */
public interface DocumentReferenceDAO extends DAO {

    public DocumentReferenceDVO findById(int referenceId) throws DAOException;

    public List findByDocumentType(String cpaId, String service, String action) throws DAOException;

    public List findAll() throws DAOException;
}
