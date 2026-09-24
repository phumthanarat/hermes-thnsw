package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * CpaPartyDAO keeps the party IDs of each CPA: Hermes routes by CPA /
 * service / action and keeps no party IDs of its own, but a partner may
 * check them. They are taken from the CPA file on upload, or entered by an
 * admin, and suggested wherever the admin console builds a message.
 */
public interface CpaPartyDAO extends DAO {

    /** @return the party IDs of the CPA, or null if none are known. */
    public CpaPartyDVO findByCpaId(String cpaId) throws DAOException;

    /** Inserts or replaces the party IDs of data's CPA. */
    public void save(CpaPartyDVO data) throws DAOException;
}
