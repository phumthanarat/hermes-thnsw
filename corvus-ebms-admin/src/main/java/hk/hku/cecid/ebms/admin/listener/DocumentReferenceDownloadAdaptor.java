package hk.hku.cecid.ebms.admin.listener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDAO;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDVO;
import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;

/**
 * DocumentReferenceDownloadAdaptor serves a reference file (.xsd/.wsdl/.xml)
 * attached to a document type in the Document Catalog, either inline
 * (mode=view, opened in a new tab) or as a download -- these are genuine
 * bare XML/XSD/WSDL files (unlike the raw wire messages RepositoryAdaptor
 * serves), so text/xml is correct for the view mode here.
 */
public class DocumentReferenceDownloadAdaptor extends HttpRequestAdaptor {

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {

        try {
            int referenceId = Integer.parseInt(request.getParameter("reference_id"));
            boolean view = "view".equalsIgnoreCase(request.getParameter("mode"));

            DocumentReferenceDAO dao = (DocumentReferenceDAO)
                    EbmsProcessor.core.dao.createDAO(DocumentReferenceDAO.class);
            DocumentReferenceDVO ref = dao.findById(referenceId);

            if (ref == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such reference file");
                return null;
            }

            response.setCharacterEncoding(null);
            if (view) {
                response.setContentType("text/xml; charset=UTF-8");
            } else {
                response.setContentType("application/download");
                response.setHeader("Content-Disposition", "attachment;filename=\"" + ref.getFilename() + "\"");
            }
            response.getOutputStream().write(ref.getContent());
        } catch (Exception e) {
            EbmsProcessor.core.log.error("Unable to serve document reference file", e);
        }
        return null;
    }
}
