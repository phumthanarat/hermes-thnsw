package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

public class CpaPartyDataSourceDVO extends DataSourceDVO implements CpaPartyDVO {

    public String getCpaId() {
        return super.getString("cpaId");
    }

    public void setCpaId(String cpaId) {
        super.setString("cpaId", cpaId);
    }

    public String getFromPartyId() {
        return super.getString("fromPartyId");
    }

    public void setFromPartyId(String fromPartyId) {
        super.setString("fromPartyId", fromPartyId);
    }

    public String getFromPartyType() {
        return super.getString("fromPartyType");
    }

    public void setFromPartyType(String fromPartyType) {
        super.setString("fromPartyType", fromPartyType);
    }

    public String getToPartyId() {
        return super.getString("toPartyId");
    }

    public void setToPartyId(String toPartyId) {
        super.setString("toPartyId", toPartyId);
    }

    public String getToPartyType() {
        return super.getString("toPartyType");
    }

    public void setToPartyType(String toPartyType) {
        super.setString("toPartyType", toPartyType);
    }

    public String getSource() {
        return super.getString("source");
    }

    public void setSource(String source) {
        super.setString("source", source);
    }
}
