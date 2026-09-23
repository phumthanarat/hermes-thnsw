package hk.hku.cecid.piazza.corvus.core.main.admin.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * ListenerPortDVO represents a row in the listener_port table: one
 * additional standalone HTTP(S) listener, bound to its own address/port,
 * that forwards whatever it receives to one of the gateway's existing
 * internal services (ebMS/AS2/SFRM) under /corvus/httpd/&lt;service&gt;/inbound.
 *
 * Typical use: a partner is given a dedicated address:port (e.g.
 * https://203.0.113.10:5443) to reach this gateway on, separate from the
 * shared admin/web port and from every other partner's own dedicated port.
 */
public interface ListenerPortDVO extends DVO {

    public String getPortId();

    public void setPortId(String portId);

    public String getName();

    public void setName(String name);

    public String getBindAddress();

    public void setBindAddress(String bindAddress);

    public int getPort();

    public void setPort(int port);

    public boolean isUseTls();

    public void setIsUseTls(boolean useTls);

    public String getTargetService();

    public void setTargetService(String targetService);

    public boolean isDisabled();

    public void setIsDisabled(boolean disabled);

    public String getDescription();

    public void setDescription(String description);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
