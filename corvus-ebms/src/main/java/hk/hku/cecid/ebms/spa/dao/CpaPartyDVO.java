package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * The party IDs of one CPA, from this gateway's point of view: "from" is
 * this party, "to" the partner. Several IDs are comma separated, with one
 * type per ID, as the ebMS sender web service takes them.
 */
public interface CpaPartyDVO extends DVO {

    public static final String SOURCE_CPA_UPLOAD = "cpa_upload";

    public static final String SOURCE_ADMIN = "admin";

    public String getCpaId();

    public void setCpaId(String cpaId);

    public String getFromPartyId();

    public void setFromPartyId(String fromPartyId);

    public String getFromPartyType();

    public void setFromPartyType(String fromPartyType);

    public String getToPartyId();

    public void setToPartyId(String toPartyId);

    public String getToPartyType();

    public void setToPartyType(String toPartyType);

    /** @return where the IDs came from: SOURCE_CPA_UPLOAD or SOURCE_ADMIN. */
    public String getSource();

    public void setSource(String source);
}
