package hk.hku.cecid.ebms.spa.listener;

import hk.hku.cecid.ebms.pkg.EbxmlMessage;


/**
 * @author Donahue Sze
 *
 */
public class EbmsRequest {

    private Object source = null;
    private EbxmlMessage message = null;
    private String createdVia = null;
    
    public EbmsRequest() {
        this(null);
    }
    
    public EbmsRequest(Object source) {
        this.source = source;
    }
    
    
    /**
     * @return Returns the msg.
     */
    public EbxmlMessage getMessage() {
        return message;
    }
    
    /**
     * @param message The msg to set.
     */
    public void setMessage(EbxmlMessage message) {
        this.message = message;
    }
    
    /**
     * @return Returns the source.
     */
    public Object getSource() {
        return source;
    }
    
    /**
     * @param source The source to set.
     */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * @return How the message was created (e.g. "webservice_api",
     *         "admin_console"), stored with the outbound message; null if
     *         not tagged.
     */
    public String getCreatedVia() {
        return createdVia;
    }

    /**
     * @param createdVia How the message was created. It is stored with the
     *            message when it is first saved, so the outbox task can't
     *            overwrite it as it could a tag written afterwards.
     */
    public void setCreatedVia(String createdVia) {
        this.createdVia = createdVia;
    }
}
