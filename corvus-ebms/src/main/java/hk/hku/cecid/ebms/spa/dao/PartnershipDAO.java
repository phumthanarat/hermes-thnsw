package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;

import java.util.List;

/**
 * @author Donahue Sze
 * 
 */
public interface PartnershipDAO extends DAO {
    public boolean findPartnershipByCPA(PartnershipDVO data)
            throws DAOException;

    public List findAllPartnerships() throws DAOException;
    
    public List findPartnershipsByCPA(PartnershipDVO data) throws DAOException;

    /**
     * Finds the enabled partnerships of a CPA, whatever their service and
     * action, ordered by partnership ID.
     */
    public List findPartnershipsByCpaId(String cpaId) throws DAOException;

    /**
     * Finds the partnership that processes a message with the CPA ID,
     * service and action of <code>data</code>. It is the exact match, except
     * that a message of the ebMS service itself (a Ping, or a Pong or Error
     * replying to one) falls back to any enabled partnership of the CPA,
     * since a CPA seldom defines a partnership for that service.
     */
    public boolean findPartnershipForMessage(PartnershipDVO data)
            throws DAOException;
}