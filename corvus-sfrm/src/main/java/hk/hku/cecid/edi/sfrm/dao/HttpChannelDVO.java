package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * HttpChannelDVO represents a standalone HTTP(S) listener the SFRM port
 * module opens on its own bind address / port, separate from the main
 * admin web management port -- decoupling message intake from the admin
 * console. Incoming requests are forwarded in-process to the existing
 * internal handler for {@link #getTargetService()} (e.g. "ebms", "as2",
 * "sfrm"), the same handler the shared admin/web port already dispatches
 * to under /corvus/httpd/&lt;service&gt;/inbound.
 *
 * @author Hermes2+ (HTTP port type)
 */
public interface HttpChannelDVO extends DVO {

    public String getChannelId();

    public void setChannelId(String channelId);

    public String getName();

    public void setName(String name);

    /** @return the local address this listener binds to, e.g. "0.0.0.0". */
    public String getBindAddress();

    public void setBindAddress(String bindAddress);

    public int getPort();

    public void setPort(int port);

    /** @return true if this listener should speak HTTPS instead of plain HTTP. */
    public boolean isUseTls();

    public void setIsUseTls(boolean useTls);

    /** @return which internal service handles requests received on this port, e.g. "ebms", "as2", "sfrm". */
    public String getTargetService();

    public void setTargetService(String targetService);

    public boolean isDisabled();

    public void setIsDisabled(boolean isDisabled);

    public String getDescription();

    public void setDescription(String description);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
