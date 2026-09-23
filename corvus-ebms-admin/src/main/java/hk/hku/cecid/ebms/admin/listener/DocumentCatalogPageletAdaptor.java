package hk.hku.cecid.ebms.admin.listener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDAO;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDVO;
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
 *
 * Also lets an admin attach reference files (.xsd/.wsdl/.xml) to a document
 * type, purely for other admins/developers to view or download from this
 * page -- kept for reference only, never used to validate live traffic.
 */
public class DocumentCatalogPageletAdaptor extends AdminPageletAdaptor {

    private static final String[] ALLOWED_EXTENSIONS = { "xsd", "wsdl", "xml" };

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/documents", "");

        if (FileUpload.isMultipartContent(request)) {
            handleUpload(request, dom);
        } else if ("post".equalsIgnoreCase(request.getMethod())) {
            String action = request.getParameter(REQ_PARAM_ACTION);
            if ("delete_reference".equalsIgnoreCase(action)) {
                handleDelete(request);
            } else if ("toggle_reference".equalsIgnoreCase(action)) {
                handleToggle(request);
            }
        }

        try {
            PartnershipDAO dao = (PartnershipDAO) EbmsProcessor.core.dao.createDAO(PartnershipDAO.class);
            List partnerships = dao.findAllPartnerships();

            DocumentReferenceDAO refDao = (DocumentReferenceDAO)
                    EbmsProcessor.core.dao.createDAO(DocumentReferenceDAO.class);
            // "cpaId\u0000service\u0000action" -> list of DocumentReferenceDVO
            Map refsByDocType = new HashMap();
            Iterator refIter = refDao.findAll().iterator();
            while (refIter.hasNext()) {
                DocumentReferenceDVO ref = (DocumentReferenceDVO) refIter.next();
                String key = refKey(ref.getCpaId(), ref.getService(), ref.getAction());
                List list = (List) refsByDocType.get(key);
                if (list == null) {
                    list = new java.util.ArrayList();
                    refsByDocType.put(key, list);
                }
                list.add(ref);
            }

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

                        List refs = (List) refsByDocType.get(refKey(cpaId, service, p.getAction()));
                        if (refs != null) {
                            for (int ri = 1; ri <= refs.size(); ri++) {
                                DocumentReferenceDVO ref = (DocumentReferenceDVO) refs.get(ri - 1);
                                String rbase = base + "/reference[" + ri + "]";
                                dom.setProperty(rbase + "/id", String.valueOf(ref.getReferenceId()));
                                dom.setProperty(rbase + "/filename", ref.getFilename());
                                dom.setProperty(rbase + "/file_type", ref.getFileType());
                                dom.setProperty(rbase + "/description",
                                        ref.getDescription() == null ? "" : ref.getDescription());
                                dom.setProperty(rbase + "/disabled", String.valueOf(ref.isDisabled()));
                            }
                        }
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

    private String refKey(String cpaId, String service, String action) {
        return cpaId + "\u0000" + service + "\u0000" + action;
    }

    private void handleUpload(HttpServletRequest request, PropertyTree dom) {
        try {
            DiskFileUpload upload = new DiskFileUpload();
            List fileItems = upload.parseRequest(request);

            String cpaId = null, service = null, action = null, description = null;
            FileItem fileItem = null;

            Iterator iter = fileItems.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                if (item.isFormField()) {
                    String field = item.getFieldName();
                    if ("cpa_id".equals(field)) cpaId = item.getString();
                    else if ("service".equals(field)) service = item.getString();
                    else if ("doc_action".equals(field)) action = item.getString();
                    else if ("description".equals(field)) description = item.getString();
                } else if (item.getName() != null && item.getName().length() > 0) {
                    fileItem = item;
                }
            }

            if (fileItem == null) {
                request.setAttribute(ATTR_MESSAGE, "Choose a .xsd, .wsdl or .xml file to upload");
                return;
            }
            if (cpaId == null || service == null || action == null
                    || cpaId.length() == 0 || service.length() == 0 || action.length() == 0) {
                request.setAttribute(ATTR_MESSAGE, "Select which document type (CPA / Service / Action) this file belongs to");
                return;
            }

            String filename = fileItem.getName();
            int dot = filename.lastIndexOf('.');
            String extension = dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
            if (!isAllowedExtension(extension)) {
                request.setAttribute(ATTR_MESSAGE, "Only .xsd, .wsdl and .xml files are accepted");
                return;
            }
            if (fileItem.getSize() == 0) {
                request.setAttribute(ATTR_MESSAGE, "The file has no content");
                return;
            }

            DocumentReferenceDAO refDao = (DocumentReferenceDAO)
                    EbmsProcessor.core.dao.createDAO(DocumentReferenceDAO.class);
            DocumentReferenceDVO ref = (DocumentReferenceDVO) refDao.createDVO();
            ref.setCpaId(cpaId);
            ref.setService(service);
            ref.setAction(action);
            ref.setFilename(filename);
            ref.setFileType(extension);
            ref.setContent(fileItem.get());
            ref.setDescription(description);
            refDao.create(ref);

            request.setAttribute(ATTR_MESSAGE, "'" + filename + "' attached to " + cpaId + " / " + service + " / " + action);
        } catch (Exception e) {
            EbmsProcessor.core.log.error("Unable to upload document reference", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to upload the file: " + e.getMessage());
        }
    }

    private boolean isAllowedExtension(String extension) {
        for (int i = 0; i < ALLOWED_EXTENSIONS.length; i++) {
            if (ALLOWED_EXTENSIONS[i].equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private void handleToggle(HttpServletRequest request) {
        try {
            int referenceId = Integer.parseInt(request.getParameter("reference_id"));
            DocumentReferenceDAO refDao = (DocumentReferenceDAO)
                    EbmsProcessor.core.dao.createDAO(DocumentReferenceDAO.class);
            DocumentReferenceDVO ref = refDao.findById(referenceId);
            if (ref != null) {
                ref.setDisabled(!ref.isDisabled());
                refDao.persist(ref);
                request.setAttribute(ATTR_MESSAGE, "'" + ref.getFilename() + "' "
                        + (ref.isDisabled() ? "disabled" : "enabled"));
            }
        } catch (Exception e) {
            EbmsProcessor.core.log.error("Unable to toggle document reference", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to update the file: " + e.getMessage());
        }
    }

    private void handleDelete(HttpServletRequest request) {
        try {
            int referenceId = Integer.parseInt(request.getParameter("reference_id"));
            DocumentReferenceDAO refDao = (DocumentReferenceDAO)
                    EbmsProcessor.core.dao.createDAO(DocumentReferenceDAO.class);
            DocumentReferenceDVO ref = refDao.findById(referenceId);
            if (ref != null) {
                refDao.remove(ref);
                request.setAttribute(ATTR_MESSAGE, "'" + ref.getFilename() + "' removed");
            }
        } catch (Exception e) {
            EbmsProcessor.core.log.error("Unable to delete document reference", e);
            request.setAttribute(ATTR_MESSAGE, "Unable to remove the file: " + e.getMessage());
        }
    }
}
