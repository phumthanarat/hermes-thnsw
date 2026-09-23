package hk.hku.cecid.piazza.corvus.core.main.admin.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * CaIssuedCertDVO represents a row in the ca_issued_cert table: one
 * certificate this gateway's internal CA has issued to a partner.
 */
public interface CaIssuedCertDVO extends DVO {

    public String getSerialNumber();

    public void setSerialNumber(String serialNumber);

    public String getSubjectDn();

    public void setSubjectDn(String subjectDn);

    public byte[] getCert();

    public void setCert(byte[] cert);

    public boolean isRevoked();

    public void setRevoked(boolean revoked);

    public Timestamp getIssuedTimestamp();

    public void setIssuedTimestamp(Timestamp issuedTimestamp);

    public Timestamp getRevokedTimestamp();

    public void setRevokedTimestamp(Timestamp revokedTimestamp);
}
