package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * FilePollingChannelDVO represents one additional directory the SFRM file
 * polling mechanism watches for outbound ".sfrm" files, besides the single
 * default outgoing-payload-repository location. Files found ready in a
 * channel's watch_path are funneled into that default location so the
 * existing, already-proven OutgoingPayloadsCollector pipeline packages and
 * sends them exactly as if they had been dropped there directly.
 *
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public interface FilePollingChannelDVO extends DVO {

    public String getChannelId();

    public void setChannelId(String channelId);

    public String getName();

    public void setName(String name);

    public String getWatchPath();

    public void setWatchPath(String watchPath);

    /**
     * @return polling interval override in ms for this channel, or -1 if
     *         not set (the collector's default execution-interval applies).
     */
    public int getPollingInterval();

    public void setPollingInterval(int pollingInterval);

    /**
     * @return max files picked up per scan for this channel, or -1 if not
     *         set (falls back to a sane default).
     */
    public int getMaxFilesPerPoll();

    public void setMaxFilesPerPoll(int maxFilesPerPoll);

    public boolean isDisabled();

    public void setIsDisabled(boolean isDisabled);

    public String getDescription();

    public void setDescription(String description);

    public Timestamp getCreatedTimestamp();

    public void setCreatedTimestamp(Timestamp createdTimestamp);
}
