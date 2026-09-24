package hk.hku.cecid.ebms.spa.dao;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

public class HousekeepingDataSourceDAO extends DataSourceDAO implements HousekeepingDAO {

    /** The settings are a single row. */
    private static final int ROW_ID = 1;

    public DVO createDVO() {
        return new HousekeepingDataSourceDVO();
    }

    public HousekeepingDVO load() throws DAOException {
        HousekeepingDataSourceDVO dvo = (HousekeepingDataSourceDVO) createDVO();
        dvo.setId(ROW_ID);
        if (!super.retrieve(dvo)) {
            dvo.setEnabled(false);
            dvo.setRetentionDays(90);
            dvo.setRunTime("02:00");
            dvo.setMessageBox(HousekeepingDVO.ALL);
            dvo.setMessageTypes(HousekeepingDVO.ALL);
            dvo.setStatuses("DL,PS");
        }
        return dvo;
    }

    public void save(HousekeepingDVO data) throws DAOException {
        HousekeepingDataSourceDVO dvo = (HousekeepingDataSourceDVO) data;
        dvo.setId(ROW_ID);
        HousekeepingDataSourceDVO existing = (HousekeepingDataSourceDVO) createDVO();
        existing.setId(ROW_ID);
        if (super.retrieve(existing)) {
            super.persist(dvo);
        } else {
            super.create(dvo);
        }
    }
}
