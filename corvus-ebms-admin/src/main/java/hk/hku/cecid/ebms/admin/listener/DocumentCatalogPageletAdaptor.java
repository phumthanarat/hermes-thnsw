package hk.hku.cecid.ebms.admin.listener;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.PartnershipDAO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

/**
 * DocumentCatalogPageletAdaptor lists the document types (service/action
 * pairs) that this Hermes instance can send or receive, grouped by CPA
 * (trading partner) and service, sourced live from the partnership table
 * rather than a hard-coded list.
 */
public class DocumentCatalogPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/documents", "");

        try {
            PartnershipDAO dao = (PartnershipDAO) EbmsProcessor.core.dao.createDAO(PartnershipDAO.class);
            List partnerships = dao.findAllPartnerships();

            // cpaId -> (service -> list of PartnershipDVO)
            Map byCpa = new LinkedHashMap();
            Iterator iter = partnerships.iterator();
            while (iter.hasNext()) {
                PartnershipDVO p = (PartnershipDVO) iter.next();
                Map byService = (Map) byCpa.get(p.getCpaId());
                if (byService == null) {
                    byService = new LinkedHashMap();
                    byCpa.put(p.getCpaId(), byService);
                }
                List actions = (List) byService.get(p.getService());
                if (actions == null) {
                    actions = new java.util.ArrayList();
                    byService.put(p.getService(), actions);
                }
                actions.add(p);
            }

            int ci = 0;
            Iterator cpaIter = byCpa.keySet().iterator();
            while (cpaIter.hasNext()) {
                ci++;
                String cpaId = (String) cpaIter.next();
                Map byService = (Map) byCpa.get(cpaId);

                dom.setProperty("cpa[" + ci + "]/id", cpaId);

                int si = 0;
                Iterator serviceIter = byService.keySet().iterator();
                while (serviceIter.hasNext()) {
                    si++;
                    String service = (String) serviceIter.next();
                    List actions = (List) byService.get(service);

                    dom.setProperty("cpa[" + ci + "]/service[" + si + "]/name", service);

                    for (int ai = 1; ai <= actions.size(); ai++) {
                        PartnershipDVO p = (PartnershipDVO) actions.get(ai - 1);
                        String base = "cpa[" + ci + "]/service[" + si + "]/action[" + ai + "]";
                        dom.setProperty(base + "/name", p.getAction());
                        dom.setProperty(base + "/partnership_id", p.getPartnershipId());
                        dom.setProperty(base + "/endpoint", p.getTransportEndpoint() == null ? "" : p.getTransportEndpoint());
                        dom.setProperty(base + "/disabled", "true".equalsIgnoreCase(p.getDisabled()) ? "true" : "false");
                    }
                }
            }

            dom.setProperty("summary/cpa_count", String.valueOf(byCpa.size()));
            dom.setProperty("summary/partnership_count", String.valueOf(partnerships.size()));
        }
        catch (DAOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to load document catalog: " + e.getMessage());
        }

        return dom.getSource();
    }
}
