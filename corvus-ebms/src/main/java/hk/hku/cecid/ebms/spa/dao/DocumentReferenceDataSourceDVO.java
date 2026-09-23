package hk.hku.cecid.ebms.spa.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * @author Hermes2+ (Document Catalog reference files)
 */
public class DocumentReferenceDataSourceDVO extends DataSourceDVO implements DocumentReferenceDVO {

    public int getReferenceId() {
        return super.getInt("referenceId");
    }

    public void setReferenceId(int referenceId) {
        super.setInt("referenceId", referenceId);
    }

    public String getCpaId() {
        return super.getString("cpaId");
    }

    public void setCpaId(String cpaId) {
        super.setString("cpaId", cpaId);
    }

    public String getService() {
        return super.getString("service");
    }

    public void setService(String service) {
        super.setString("service", service);
    }

    public String getAction() {
        return super.getString("action");
    }

    public void setAction(String action) {
        super.setString("action", action);
    }

    public String getFilename() {
        return super.getString("filename");
    }

    public void setFilename(String filename) {
        super.setString("filename", filename);
    }

    public String getFileType() {
        return super.getString("fileType");
    }

    public void setFileType(String fileType) {
        super.setString("fileType", fileType);
    }

    public byte[] getContent() {
        return (byte[]) super.get("content");
    }

    public void setContent(byte[] content) {
        super.put("content", content);
    }

    public String getDescription() {
        return super.getString("description");
    }

    public void setDescription(String description) {
        super.setString("description", description);
    }

    public boolean isDisabled() {
        return super.getBoolean("disabled");
    }

    public void setDisabled(boolean disabled) {
        super.setBoolean("disabled", disabled);
    }

    public Timestamp getUploadedTimestamp() {
        return (Timestamp) super.get("uploadedTimestamp");
    }

    public void setUploadedTimestamp(Timestamp uploadedTimestamp) {
        super.put("uploadedTimestamp", uploadedTimestamp);
    }
}
