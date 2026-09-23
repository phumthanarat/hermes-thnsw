package hk.hku.cecid.piazza.corvus.core.main.admin.dao.ds;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.corvus.core.main.admin.dao.CaIssuedCertDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * CaIssuedCertDSDVO is the data source implementation of CaIssuedCertDVO.
 */
public class CaIssuedCertDSDVO extends DataSourceDVO implements CaIssuedCertDVO {

    private static final long serialVersionUID = 1L;

    public String getSerialNumber() {
        return super.getString("serialNumber");
    }

    public void setSerialNumber(String serialNumber) {
        super.setString("serialNumber", serialNumber);
    }

    public String getSubjectDn() {
        return super.getString("subjectDn");
    }

    public void setSubjectDn(String subjectDn) {
        super.setString("subjectDn", subjectDn);
    }

    public byte[] getCert() {
        return (byte[]) super.get("cert");
    }

    public void setCert(byte[] cert) {
        super.put("cert", cert);
    }

    public boolean isRevoked() {
        return super.getBoolean("revoked");
    }

    public void setRevoked(boolean revoked) {
        super.setBoolean("revoked", revoked);
    }

    public Timestamp getIssuedTimestamp() {
        return (Timestamp) super.get("issuedTimestamp");
    }

    public void setIssuedTimestamp(Timestamp issuedTimestamp) {
        super.put("issuedTimestamp", issuedTimestamp);
    }

    public Timestamp getRevokedTimestamp() {
        return (Timestamp) super.get("revokedTimestamp");
    }

    public void setRevokedTimestamp(Timestamp revokedTimestamp) {
        super.put("revokedTimestamp", revokedTimestamp);
    }
}
