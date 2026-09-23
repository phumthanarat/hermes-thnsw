package hk.hku.cecid.ebms.spa.dao;

import java.util.List;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * @author Hermes2+ (Document Catalog reference files)
 */
public class DocumentReferenceDataSourceDAO extends DataSourceDAO implements DocumentReferenceDAO {

    public DVO createDVO() {
        return new DocumentReferenceDataSourceDVO();
    }

    public DocumentReferenceDVO findById(int referenceId) throws DAOException {
        DocumentReferenceDVO dvo = (DocumentReferenceDVO) createDVO();
        dvo.setReferenceId(referenceId);
        if (super.retrieve((DocumentReferenceDataSourceDVO) dvo)) {
            return dvo;
        }
        return null;
    }

    public List findByDocumentType(String cpaId, String service, String action) throws DAOException {
        return super.find("find_by_document_type", new Object[] { cpaId, service, action });
    }

    public List findAll() throws DAOException {
        return super.find("find_all", null);
    }
}
