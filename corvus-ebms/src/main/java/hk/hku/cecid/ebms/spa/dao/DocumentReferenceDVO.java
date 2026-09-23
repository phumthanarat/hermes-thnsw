package hk.hku.cecid.ebms.spa.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * DocumentReferenceDVO represents a row in the document_reference table:
 * one reference file (.xsd/.wsdl/.xml) attached to a document type
 * (cpa_id/service/action) in the Document Catalog admin page.
 */
public interface DocumentReferenceDVO extends DVO {

    public int getReferenceId();

    public void setReferenceId(int referenceId);

    public String getCpaId();

    public void setCpaId(String cpaId);

    public String getService();

    public void setService(String service);

    public String getAction();

    public void setAction(String action);

    public String getFilename();

    public void setFilename(String filename);

    public String getFileType();

    public void setFileType(String fileType);

    public byte[] getContent();

    public void setContent(byte[] content);

    public String getDescription();

    public void setDescription(String description);

    public boolean isDisabled();

    public void setDisabled(boolean disabled);

    public Timestamp getUploadedTimestamp();

    public void setUploadedTimestamp(Timestamp uploadedTimestamp);
}
