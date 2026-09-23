package hk.hku.cecid.edi.sfrm.admin.listener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hk.hku.cecid.edi.sfrm.com.NamedPayloads;
import hk.hku.cecid.edi.sfrm.com.PackagedPayloadsRepository;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDAO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;

/**
 * RepositoryAdaptor serves a SFRM message's packaged payload (the
 * &lt;partnership_id&gt;$&lt;message_id&gt;.sfrm zip SFRMMessage.pack()
 * produces -- see OutgoingMessageHandler/IncomingMessageHandler) for
 * download or inline viewing.
 *
 * <p>Unlike ebMS and AS2plus, which persist every message's raw content in
 * a database repository table, SFRM's payload repositories
 * (outgoing-payload-repository / incoming-payload-repository, on disk under
 * hermes_home/repository/sfrm-{outgoing,incoming}-repository) are transient
 * staging areas: {@link NamedPayloads#clearPayloadCache()} deletes a
 * message's packaged file once it's no longer needed (sent and
 * acknowledged, or received and processed). So this only finds content for
 * a message still in flight or stuck -- which is exactly when an admin
 * would most want to inspect it -- not for a message that completed
 * normally some time ago; that's expected, not a bug, and the response
 * says so explicitly rather than returning an empty/broken page.
 */
public class RepositoryAdaptor extends HttpRequestAdaptor {

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {

        String messageId = request.getParameter("message_id");
        String messageBox = request.getParameter("message_box");
        boolean view = "view".equalsIgnoreCase(request.getParameter("mode"));

        try {
            SFRMMessageDAO messageDAO = (SFRMMessageDAO)
                    SFRMProcessor.getInstance().getDAOFactory().createDAO(SFRMMessageDAO.class);
            SFRMMessageDVO messageDVO = (SFRMMessageDVO) messageDAO.createDVO();
            messageDVO.setMessageId(messageId);
            messageDVO.setMessageBox(messageBox);

            if (!messageDAO.retrieve(messageDVO)) {
                writeUnavailable(response, "No such message.");
                return null;
            }

            PackagedPayloadsRepository repository = SFRMConstant.MSGBOX_OUT.equals(messageBox)
                    ? SFRMProcessor.getInstance().getOutgoingRepository()
                    : (PackagedPayloadsRepository) SFRMProcessor.getInstance().getIncomingRepository();

            String expectedName = messageDVO.getPartnershipId() + "$" + messageId
                    + PackagedPayloadsRepository.PACKAGE_EXT;

            NamedPayloads found = null;
            Collection payloads = repository.getPayloads();
            for (Iterator it = payloads.iterator(); it.hasNext(); ) {
                NamedPayloads candidate = (NamedPayloads) it.next();
                if (expectedName.equals(candidate.getOriginalRootname())) {
                    found = candidate;
                    break;
                }
            }

            if (found == null) {
                writeUnavailable(response, "This message's packaged payload is no longer on disk -- "
                        + "SFRM clears it from the repository once the message has been fully sent/"
                        + "acknowledged or received/processed, so this is only available for a "
                        + "message still in flight or stuck, not one that completed normally some "
                        + "time ago.");
                return null;
            }

            byte[] content = readAll(found.load());

            response.setCharacterEncoding(null);
            if (view) {
                response.setContentType("text/plain; charset=UTF-8");
            } else {
                response.setContentType("application/download");
                response.setHeader("Content-Disposition",
                        "attachment;filename=\"" + expectedName + "\"");
            }
            response.getOutputStream().write(content);

        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error("Unable to serve SFRM repository content", e);
        }
        return null;
    }

    private void writeUnavailable(HttpServletResponse response, String message) throws Exception {
        response.setContentType("text/plain; charset=UTF-8");
        response.getOutputStream().write(message.getBytes("UTF-8"));
    }

    private byte[] readAll(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buf.write(chunk, 0, read);
            }
            return buf.toByteArray();
        } finally {
            in.close();
        }
    }
}
