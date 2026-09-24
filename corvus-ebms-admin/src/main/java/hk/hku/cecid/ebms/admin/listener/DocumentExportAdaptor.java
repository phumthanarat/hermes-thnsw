package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDAO;
import hk.hku.cecid.ebms.spa.dao.DocumentReferenceDVO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDAO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDVO;
import hk.hku.cecid.piazza.commons.servlet.RequestListenerException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpRequestAdaptor;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Exports ready-to-use integration files for one document type (a
 * partnership's CPA / Service / Action) from the Document Catalog:
 * <ul>
 * <li>wsdl - Hermes' ebMS sender web service, pointing at this server, with
 * the request schema for this document type</li>
 * <li>xsd - that request schema, with CPA ID / Service / Action fixed</li>
 * <li>soap - a sample SOAP request to the sender web service (the business
 * document goes as a SOAP attachment)</li>
 * <li>xml - a sample of the ebXML message Hermes then sends to the partner
 * MSH</li>
 * <li>zip - all of the above plus the reference files attached to this
 * document type</li>
 * </ul>
 * Party IDs are suggested from the CPA's latest message, since Hermes keeps
 * none per CPA; when unknown they are left as placeholders.
 */
public class DocumentExportAdaptor extends HttpRequestAdaptor {

    private static final String SENDER_NAMESPACE = "http://service.ebms.edi.cecid.hku.hk/";

    private static final String UNKNOWN_FROM_PARTY_ID = "YOUR_PARTY_ID";

    private static final String UNKNOWN_TO_PARTY_ID = "PARTNER_PARTY_ID";

    public String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws RequestListenerException {
        try {
            String partnershipId = request.getParameter("partnership_id");
            String format = request.getParameter("format");

            PartnershipDAO dao = (PartnershipDAO) EbmsProcessor.core.dao
                    .createDAO(PartnershipDAO.class);
            PartnershipDVO partnership = (PartnershipDVO) dao.createDVO();
            partnership.setPartnershipId(partnershipId);
            if (partnershipId == null || !dao.retrieve(partnership)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such document type");
                return null;
            }

            Export export = new Export(partnership, CpaPartyIds.suggest(partnership.getCpaId()),
                    senderUrl(request));
            String base = fileName(partnership.getCpaId() + "-" + partnership.getAction());

            if ("zip".equalsIgnoreCase(format)) {
                sendZip(export, base, response);
                return null;
            }
            String content;
            String extension;
            if ("wsdl".equalsIgnoreCase(format)) {
                content = export.wsdl();
                extension = ".wsdl";
            } else if ("xsd".equalsIgnoreCase(format)) {
                content = export.xsd();
                extension = ".xsd";
            } else if ("soap".equalsIgnoreCase(format)) {
                content = export.soapRequest();
                extension = "-soap-request.xml";
            } else if ("xml".equalsIgnoreCase(format)) {
                content = export.ebxmlMessage();
                extension = "-ebxml-message.xml";
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "format must be wsdl, xsd, soap, xml or zip");
                return null;
            }
            byte[] bytes = content.getBytes("UTF-8");
            response.setCharacterEncoding(null);
            response.setContentType("application/download");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + base + extension + "\"");
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (Exception e) {
            EbmsProcessor.core.log.error("Unable to export document type files", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to export: " + e.getMessage());
            } catch (IOException ignored) {
                // the response is already committed
            }
        }
        return null;
    }

    private void sendZip(Export export, String base, HttpServletResponse response)
            throws Exception {
        response.setCharacterEncoding(null);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + base + ".zip\"");
        OutputStream out = response.getOutputStream();
        ZipOutputStream zip = new ZipOutputStream(out);
        addEntry(zip, base + ".wsdl", export.wsdl().getBytes("UTF-8"));
        addEntry(zip, base + ".xsd", export.xsd().getBytes("UTF-8"));
        addEntry(zip, base + "-soap-request.xml", export.soapRequest().getBytes("UTF-8"));
        addEntry(zip, base + "-ebxml-message.xml", export.ebxmlMessage().getBytes("UTF-8"));

        PartnershipDVO p = export.partnership;
        DocumentReferenceDAO refDao = (DocumentReferenceDAO) EbmsProcessor.core.dao
                .createDAO(DocumentReferenceDAO.class);
        for (Iterator i = refDao.findAll().iterator(); i.hasNext();) {
            DocumentReferenceDVO ref = (DocumentReferenceDVO) i.next();
            if (!ref.isDisabled() && p.getCpaId().equals(ref.getCpaId())
                    && p.getService().equals(ref.getService())
                    && p.getAction().equals(ref.getAction())) {
                addEntry(zip, "reference/" + fileName(ref.getFilename()), ref.getContent());
            }
        }
        zip.finish();
    }

    private void addEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private String senderUrl(HttpServletRequest request) {
        StringBuffer url = new StringBuffer();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (!(("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443))) {
            url.append(':').append(port);
        }
        return url.append(request.getContextPath()).append("/httpd/ebms/sender").toString();
    }

    /** Keeps a download/ZIP entry name to safe characters. */
    private static String fileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuffer out = new StringBuffer(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '&': out.append("&amp;"); break;
            case '<': out.append("&lt;"); break;
            case '>': out.append("&gt;"); break;
            case '"': out.append("&quot;"); break;
            case '\'': out.append("&apos;"); break;
            default: out.append(c);
            }
        }
        return out.toString();
    }

    /** The generated files for one partnership. */
    private static class Export {

        final PartnershipDVO partnership;

        final String fromPartyId;

        final String toPartyId;

        final boolean partyIdsKnown;

        final String senderUrl;

        Export(PartnershipDVO partnership, CpaPartyIds partyIds, String senderUrl) {
            this.partnership = partnership;
            this.partyIdsKnown = partyIds.from.length() > 0 && partyIds.to.length() > 0;
            this.fromPartyId = partyIds.from.length() > 0 ? partyIds.from : UNKNOWN_FROM_PARTY_ID;
            this.toPartyId = partyIds.to.length() > 0 ? partyIds.to : UNKNOWN_TO_PARTY_ID;
            this.senderUrl = senderUrl;
        }

        private String header(String what) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<!--\n"
                    + "  " + what + "\n"
                    + "  CPA ID:  " + commentSafe(partnership.getCpaId()) + "\n"
                    + "  Service: " + commentSafe(partnership.getService()) + "\n"
                    + "  Action:  " + commentSafe(partnership.getAction()) + "\n"
                    + "  Exported from the Hermes admin console (Documents).\n"
                    + (partyIdsKnown ? ""
                            : "  Party IDs are unknown for this CPA (no message yet): replace "
                                    + UNKNOWN_FROM_PARTY_ID + " / " + UNKNOWN_TO_PARTY_ID
                                    + "\n  with the PartyId values in the CPA.\n")
                    + "-->\n";
        }

        /** "--" may not appear inside an XML comment. */
        private String commentSafe(String value) {
            return value == null ? "" : value.replaceAll("--", "- -");
        }

        String xsd() {
            return header("Schema of a Hermes ebMS sender request for this document type.")
                    + schema("");
        }

        private String schema(String indent) {
            String i = indent;
            return i + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
                    + i + "           targetNamespace=\"" + SENDER_NAMESPACE + "\"\n"
                    + i + "           elementFormDefault=\"qualified\">\n"
                    + fixed(i, "cpaId", partnership.getCpaId())
                    + fixed(i, "service", partnership.getService())
                    + fixed(i, "action", partnership.getAction())
                    + element(i, "convId", "Conversation ID, chosen by the sender", false)
                    + element(i, "fromPartyId", "Sender party ID(s), comma separated", false)
                    + element(i, "fromPartyType", "Sender party type(s), one per ID", false)
                    + element(i, "toPartyId", "Receiver party ID(s), comma separated", false)
                    + element(i, "toPartyType", "Receiver party type(s), one per ID", false)
                    + element(i, "refToMessageId", "Message ID this one replies to", true)
                    + element(i, "serviceType", "ebXML Service type attribute", true)
                    + i + "  <xs:element name=\"message_id\" type=\"xs:string\">\n"
                    + i + "    <xs:annotation><xs:documentation>ID of the message Hermes queued (response)</xs:documentation></xs:annotation>\n"
                    + i + "  </xs:element>\n"
                    + i + "</xs:schema>\n";
        }

        private String fixed(String i, String name, String value) {
            return i + "  <xs:element name=\"" + name + "\" type=\"xs:string\" fixed=\""
                    + escape(value) + "\"/>\n";
        }

        private String element(String i, String name, String doc, boolean optional) {
            return i + "  <xs:element name=\"" + name + "\" type=\"xs:string\">\n"
                    + i + "    <xs:annotation><xs:documentation>" + escape(doc)
                    + (optional ? " (optional)" : "") + "</xs:documentation></xs:annotation>\n"
                    + i + "  </xs:element>\n";
        }

        String wsdl() {
            String[] parts = { "cpaId", "service", "action", "convId", "fromPartyId",
                    "fromPartyType", "toPartyId", "toPartyType", "refToMessageId",
                    "serviceType" };
            StringBuffer request = new StringBuffer();
            for (int i = 0; i < parts.length; i++) {
                request.append("    <part name=\"").append(parts[i])
                        .append("\" element=\"p:").append(parts[i]).append("\"/>\n");
            }
            return header("WSDL of the Hermes ebMS sender web service for this document type.\n"
                    + "  Send the business document as a SOAP attachment (SOAP with Attachments).")
                    + "<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\"\n"
                    + "             xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n"
                    + "             xmlns:p=\"" + SENDER_NAMESPACE + "\"\n"
                    + "             targetNamespace=\"" + SENDER_NAMESPACE + "\">\n"
                    + "  <types>\n"
                    + schema("    ")
                    + "  </types>\n"
                    + "  <message name=\"EbmsRequestMsg\">\n"
                    + request
                    + "  </message>\n"
                    + "  <message name=\"EbmsResponseMsg\">\n"
                    + "    <part name=\"message_id\" element=\"p:message_id\"/>\n"
                    + "  </message>\n"
                    + "  <portType name=\"EbmsSend\">\n"
                    + "    <operation name=\"Request\">\n"
                    + "      <input message=\"p:EbmsRequestMsg\"/>\n"
                    + "      <output message=\"p:EbmsResponseMsg\"/>\n"
                    + "    </operation>\n"
                    + "  </portType>\n"
                    + "  <binding name=\"EbmsSoapHttpSend\" type=\"p:EbmsSend\">\n"
                    + "    <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>\n"
                    + "    <operation name=\"Request\">\n"
                    + "      <soap:operation soapAction=\"ebmssend\" style=\"document\"/>\n"
                    + "      <input><soap:body use=\"literal\"/></input>\n"
                    + "      <output><soap:body use=\"literal\"/></output>\n"
                    + "    </operation>\n"
                    + "  </binding>\n"
                    + "  <service name=\"EbmsOutbound\">\n"
                    + "    <documentation>Send " + escape(partnership.getAction()) + " ("
                    + escape(partnership.getService()) + ") under CPA "
                    + escape(partnership.getCpaId()) + "</documentation>\n"
                    + "    <port name=\"EbmsSend\" binding=\"p:EbmsSoapHttpSend\">\n"
                    + "      <soap:address location=\"" + escape(senderUrl) + "\"/>\n"
                    + "    </port>\n"
                    + "  </service>\n"
                    + "</definitions>\n";
        }

        String soapRequest() {
            return header("Sample request to the Hermes ebMS sender web service:\n"
                    + "  POST " + commentSafe(senderUrl) + "  (SOAPAction: ebmssend)\n"
                    + "  Attach the business document as a SOAP attachment (multipart/related);\n"
                    + "  Hermes answers with the queued message_id.")
                    + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
                    + "                   xmlns:p=\"" + SENDER_NAMESPACE + "\">\n"
                    + "  <SOAP-ENV:Body>\n"
                    + "    <p:cpaId>" + escape(partnership.getCpaId()) + "</p:cpaId>\n"
                    + "    <p:service>" + escape(partnership.getService()) + "</p:service>\n"
                    + "    <p:action>" + escape(partnership.getAction()) + "</p:action>\n"
                    + "    <p:convId>CONVERSATION_ID</p:convId>\n"
                    + "    <p:fromPartyId>" + escape(fromPartyId) + "</p:fromPartyId>\n"
                    + "    <p:fromPartyType>string</p:fromPartyType>\n"
                    + "    <p:toPartyId>" + escape(toPartyId) + "</p:toPartyId>\n"
                    + "    <p:toPartyType>string</p:toPartyType>\n"
                    + "    <p:refToMessageId></p:refToMessageId>\n"
                    + "  </SOAP-ENV:Body>\n"
                    + "</SOAP-ENV:Envelope>\n";
        }

        String ebxmlMessage() {
            SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            StringBuffer extras = new StringBuffer();
            if ("always".equalsIgnoreCase(partnership.getAckRequested())) {
                extras.append("    <eb:AckRequested SOAP-ENV:mustUnderstand=\"1\" eb:version=\"2.0\" eb:signed=\"")
                        .append("always".equalsIgnoreCase(partnership.getAckSignRequested()))
                        .append("\" SOAP-ENV:actor=\"urn:oasis:names:tc:ebxml-msg:actor:toPartyMSH\"/>\n");
            }
            String syncReply = partnership.getSyncReplyMode();
            if (syncReply != null && !"none".equalsIgnoreCase(syncReply)) {
                extras.append("    <eb:SyncReply SOAP-ENV:mustUnderstand=\"1\" eb:version=\"2.0\"")
                        .append(" SOAP-ENV:actor=\"http://schemas.xmlsoap.org/soap/actor/next\"/>\n");
            }
            return header("Sample of the ebXML (ebMS 2.0) message Hermes sends to the partner MSH\n"
                    + "  at " + commentSafe(partnership.getTransportEndpoint()) + "\n"
                    + "  The business document travels as MIME part cid:payload-0.")
                    + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
                    + "                   xmlns:eb=\"http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd\"\n"
                    + "                   xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n"
                    + "  <SOAP-ENV:Header>\n"
                    + "    <eb:MessageHeader SOAP-ENV:mustUnderstand=\"1\" eb:version=\"2.0\">\n"
                    + "      <eb:From><eb:PartyId eb:type=\"string\">" + escape(fromPartyId) + "</eb:PartyId></eb:From>\n"
                    + "      <eb:To><eb:PartyId eb:type=\"string\">" + escape(toPartyId) + "</eb:PartyId></eb:To>\n"
                    + "      <eb:CPAId>" + escape(partnership.getCpaId()) + "</eb:CPAId>\n"
                    + "      <eb:ConversationId>CONVERSATION_ID</eb:ConversationId>\n"
                    + "      <eb:Service>" + escape(partnership.getService()) + "</eb:Service>\n"
                    + "      <eb:Action>" + escape(partnership.getAction()) + "</eb:Action>\n"
                    + "      <eb:MessageData>\n"
                    + "        <eb:MessageId>MESSAGE_ID</eb:MessageId>\n"
                    + "        <eb:Timestamp>" + utc.format(new Date()) + "</eb:Timestamp>\n"
                    + "      </eb:MessageData>\n"
                    + ("always".equalsIgnoreCase(partnership.getDupElimination())
                            ? "      <eb:DuplicateElimination/>\n" : "")
                    + "    </eb:MessageHeader>\n"
                    + extras
                    + "  </SOAP-ENV:Header>\n"
                    + "  <SOAP-ENV:Body>\n"
                    + "    <eb:Manifest eb:version=\"2.0\">\n"
                    + "      <eb:Reference xlink:href=\"cid:payload-0\" xlink:type=\"simple\"/>\n"
                    + "    </eb:Manifest>\n"
                    + "  </SOAP-ENV:Body>\n"
                    + "</SOAP-ENV:Envelope>\n";
        }
    }
}
