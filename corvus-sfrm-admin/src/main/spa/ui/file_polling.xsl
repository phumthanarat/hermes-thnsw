<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/file_polling">

<div class="stat-grid">
  <div class="stat-card">
    <h3>File Polling</h3>
    <div class="stat-row"><span class="label">How it works</span></div>
    <div class="stat-row" style="border-bottom:none;">
      <span style="font-size:12px; color:var(--text-soft);">
        Drop a file named <code>&lt;partnership_id&gt;$&lt;message_id&gt;[original_filename].sfrm</code>
        into the Outgoing Location below and it is picked up, packaged and sent automatically --
        no Web Service API call needed.
      </span>
    </div>
  </div>
</div>

<form name="filePollingForm" method="post" action="file_polling" onSubmit="return confirm('Are you sure to update the file polling settings? This will not take effect until the SFRM plugin restarts.');">

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Directories</th>
  </tr>
  <tr>
    <td width="40%">Outgoing Location<br/><small><font color="gray">Where outbound .sfrm files are dropped to be sent</font></small></td>
    <td width="60%">
      <input type="text" size="60">
        <xsl:attribute name="name">property:/file_polling/outgoing_location</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="./outgoing_location" /></xsl:attribute>
      </input>
    </td>
  </tr>
  <tr>
    <td width="40%">Incoming Location<br/><small><font color="gray">Where received payloads are unpacked to</font></small></td>
    <td width="60%">
      <input type="text" size="60">
        <xsl:attribute name="name">property:/file_polling/incoming_location</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="./incoming_location" /></xsl:attribute>
      </input>
    </td>
  </tr>

  <tr><td/><td/></tr>
  <tr>
    <th colspan="2" align="left">Polling Behaviour</th>
  </tr>
  <tr>
    <td width="40%">Polling Interval (ms)<br/><small><font color="gray">How often the Outgoing Location is scanned for new files</font></small></td>
    <td width="60%">
      <input type="text" size="10">
        <xsl:attribute name="name">property:/file_polling/polling_interval</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="./polling_interval" /></xsl:attribute>
      </input>
    </td>
  </tr>
  <tr>
    <td width="40%">Max Files Per Poll<br/><small><font color="gray">How many ready files are picked up in a single scan</font></small></td>
    <td width="60%">
      <input type="text" size="10">
        <xsl:attribute name="name">property:/file_polling/max_files_per_poll</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="./max_files_per_poll" /></xsl:attribute>
      </input>
    </td>
  </tr>
  <tr>
    <td width="40%">Max Concurrent Threads<br/><small><font color="gray">How many files can be packaged/sent at the same time</font></small></td>
    <td width="60%">
      <input type="text" size="10">
        <xsl:attribute name="name">property:/file_polling/max_threads</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="./max_threads" /></xsl:attribute>
      </input>
    </td>
  </tr>

  <tr>
    <td width="40%"></td>
    <td width="60%">
      <br/>
      <input type="hidden" name="action" value="update" />
      <input type="Submit" value="Update" />
      <br/>
    </td>
  </tr>
</table>

</form>

<br/>

<div class="stat-grid">
  <div class="stat-card">
    <h3>Additional Watch Paths (Channels)</h3>
    <div class="stat-row"><span class="label">Total Channels</span><span class="value big"><xsl:value-of select="count(channels/channel)" /></span></div>
  </div>
</div>

<xsl:if test="count(channels/channel) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Channel ID</th>
    <th>Name</th>
    <th>Watch Path</th>
    <th>Interval</th>
    <th>Max/Poll</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="channels/channel">
  <tr>
    <td><code><xsl:value-of select="./channel_id" /></code></td>
    <td><xsl:value-of select="./name" /><br/><small><font color="gray"><xsl:value-of select="./description" /></font></small></td>
    <td style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./watch_path" /></td>
    <td><xsl:value-of select="./polling_interval" /></td>
    <td><xsl:value-of select="./max_files_per_poll" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./is_disabled = 'true'">
          <span class="badge badge-neutral">Disabled</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Enabled</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="white-space:nowrap;">
      <form method="post" action="file_polling" style="display:inline;">
        <input type="hidden" name="action" value="toggle_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="file_polling" style="display:inline;" onSubmit="return confirm('Delete this channel?');">
        <input type="hidden" name="action" value="delete_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<div class="stat-grid">
  <div class="stat-card">
    <h3>FTP Channels</h3>
    <div class="stat-row"><span class="label">Total FTP Channels</span><span class="value big"><xsl:value-of select="count(ftp_channels/channel)" /></span></div>
  </div>
</div>

<xsl:if test="count(ftp_channels/channel) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Channel ID</th>
    <th>Name</th>
    <th>Host</th>
    <th>Remote Path</th>
    <th>Interval</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="ftp_channels/channel">
  <tr>
    <td><code><xsl:value-of select="./channel_id" /></code></td>
    <td><xsl:value-of select="./name" /><br/><small><font color="gray"><xsl:value-of select="./description" /></font></small></td>
    <td style="font-size:12px; color:var(--text-soft);">
      <xsl:value-of select="./username" />@<xsl:value-of select="./host" />:<xsl:value-of select="./port" />
      <xsl:if test="./use_tls = 'true'">
        <br/>
        <xsl:choose>
          <xsl:when test="./tls_pinned = 'true'"><span class="badge badge-success">FTPS (pinned)</span></xsl:when>
          <xsl:otherwise><span class="badge badge-info">FTPS (pins on next connect)</span></xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </td>
    <td style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./remote_path" /></td>
    <td><xsl:value-of select="./polling_interval" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./is_disabled = 'true'">
          <span class="badge badge-neutral">Disabled</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Enabled</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="white-space:nowrap;">
      <form method="post" action="file_polling" style="display:inline;">
        <input type="hidden" name="action" value="toggle_ftp_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="file_polling" style="display:inline;" onSubmit="return confirm('Delete this FTP channel?');">
        <input type="hidden" name="action" value="delete_ftp_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<div class="stat-grid">
  <div class="stat-card">
    <h3>SFTP Channels</h3>
    <div class="stat-row"><span class="label">Total SFTP Channels</span><span class="value big"><xsl:value-of select="count(sftp_channels/channel)" /></span></div>
  </div>
</div>

<xsl:if test="count(sftp_channels/channel) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Channel ID</th>
    <th>Name</th>
    <th>Host</th>
    <th>Remote Path</th>
    <th>Interval</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="sftp_channels/channel">
  <tr>
    <td><code><xsl:value-of select="./channel_id" /></code></td>
    <td><xsl:value-of select="./name" /><br/><small><font color="gray"><xsl:value-of select="./description" /></font></small></td>
    <td style="font-size:12px; color:var(--text-soft);">
      <xsl:value-of select="./username" />@<xsl:value-of select="./host" />:<xsl:value-of select="./port" />
      <br/>
      <xsl:choose>
        <xsl:when test="./host_key_pinned = 'true'"><span class="badge badge-success">Host key pinned</span></xsl:when>
        <xsl:otherwise><span class="badge badge-info">Pins on next connect</span></xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./remote_path" /></td>
    <td><xsl:value-of select="./polling_interval" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./is_disabled = 'true'">
          <span class="badge badge-neutral">Disabled</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Enabled</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="white-space:nowrap;">
      <form method="post" action="file_polling" style="display:inline;">
        <input type="hidden" name="action" value="toggle_sftp_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="file_polling" style="display:inline;" onSubmit="return confirm('Delete this SFTP channel?');">
        <input type="hidden" name="action" value="delete_sftp_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<div class="stat-grid">
  <div class="stat-card">
    <h3>Mail Channels</h3>
    <div class="stat-row"><span class="label">Total Mail Channels</span><span class="value big"><xsl:value-of select="count(mail_channels/channel)" /></span></div>
  </div>
</div>

<xsl:if test="count(mail_channels/channel) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Channel ID</th>
    <th>Name</th>
    <th>Account</th>
    <th>Folder</th>
    <th>Interval</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="mail_channels/channel">
  <tr>
    <td><code><xsl:value-of select="./channel_id" /></code></td>
    <td><xsl:value-of select="./name" /><br/><small><font color="gray"><xsl:value-of select="./description" /></font></small></td>
    <td style="font-size:12px; color:var(--text-soft);">
      <xsl:value-of select="./username" />@<xsl:value-of select="./host" /><xsl:if test="./port != 'default'">:<xsl:value-of select="./port" /></xsl:if>
      <br/>
      <span class="badge badge-info"><xsl:value-of select="./protocol" /><xsl:if test="./use_ssl = 'true'">S</xsl:if></span>
    </td>
    <td style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./folder" /></td>
    <td><xsl:value-of select="./polling_interval" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./is_disabled = 'true'">
          <span class="badge badge-neutral">Disabled</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Enabled</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="white-space:nowrap;">
      <form method="post" action="file_polling" style="display:inline;">
        <input type="hidden" name="action" value="toggle_mail_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="file_polling" style="display:inline;" onSubmit="return confirm('Delete this mail channel?');">
        <input type="hidden" name="action" value="delete_mail_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<div class="stat-grid">
  <div class="stat-card">
    <h3>HTTP Channels</h3>
    <div class="stat-row"><span class="label">Total HTTP Channels</span><span class="value big"><xsl:value-of select="count(http_channels/channel)" /></span></div>
  </div>
</div>

<xsl:if test="count(http_channels/channel) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Channel ID</th>
    <th>Name</th>
    <th>Listens On</th>
    <th>Target Service</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="http_channels/channel">
  <tr>
    <td><code><xsl:value-of select="./channel_id" /></code></td>
    <td><xsl:value-of select="./name" /><br/><small><font color="gray"><xsl:value-of select="./description" /></font></small></td>
    <td style="font-size:12px; color:var(--text-soft);">
      <xsl:choose>
        <xsl:when test="./use_tls = 'true'">https://</xsl:when>
        <xsl:otherwise>http://</xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="./bind_address" />:<xsl:value-of select="./port" />
    </td>
    <td><span class="badge badge-info"><xsl:value-of select="./target_service" /></span></td>
    <td>
      <xsl:choose>
        <xsl:when test="./is_disabled = 'true'">
          <span class="badge badge-neutral">Disabled</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Enabled</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td style="white-space:nowrap;">
      <form method="post" action="file_polling" style="display:inline;">
        <input type="hidden" name="action" value="toggle_http_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="file_polling" style="display:inline;" onSubmit="return confirm('Delete this HTTP channel?');">
        <input type="hidden" name="action" value="delete_http_channel" />
        <input type="hidden" name="channel_id"><xsl:attribute name="value"><xsl:value-of select="./channel_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Add Port</th>
  </tr>
  <tr>
    <td width="40%">Select Type of Port to Configure</td>
    <td width="60%">
      <div>
        <label><input type="radio" name="port_type" value="file_polling" checked="checked" onclick="selectPortType('file_polling')" /> File Polling</label>
      </div>
      <div>
        <label><input type="radio" name="port_type" value="mail" onclick="selectPortType('mail')" /> Mail (POP3 / IMAP)</label>
      </div>
      <div>
        <label><input type="radio" name="port_type" value="ftp" onclick="selectPortType('ftp')" /> FTP / FTPS</label>
      </div>
      <div>
        <label><input type="radio" name="port_type" value="sftp" onclick="selectPortType('sftp')" /> SFTP</label>
      </div>
      <div>
        <label><input type="radio" name="port_type" value="http" onclick="selectPortType('http')" /> HTTP / HTTPS</label>
      </div>
    </td>
  </tr>
</table>

<div id="portTypeFilePolling">
<form name="addChannelForm" method="post" action="file_polling">
<input type="hidden" name="action" value="add_channel" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <td width="40%">Channel ID</td>
    <td width="60%"><input type="text" name="channel_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name</td>
    <td width="60%"><input type="text" name="channel_name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Watch Path</td>
    <td width="60%"><input type="text" name="channel_watch_path" size="60" /></td>
  </tr>
  <tr>
    <td width="40%">Polling Interval (ms)<br/><small><font color="gray">Leave blank to use the default above</font></small></td>
    <td width="60%"><input type="text" name="channel_polling_interval" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Max Files Per Poll<br/><small><font color="gray">Leave blank to use the default above</font></small></td>
    <td width="60%"><input type="text" name="channel_max_files_per_poll" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="channel_description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Add Port" /><br/></td>
  </tr>
</table>
</form>
</div>

<div id="portTypeMail" style="display:none;">
<form name="addMailChannelForm" method="post" action="file_polling">
<input type="hidden" name="action" value="add_mail_channel" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <td width="40%">Channel ID</td>
    <td width="60%"><input type="text" name="mail_channel_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name</td>
    <td width="60%"><input type="text" name="mail_name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Protocol</td>
    <td width="60%">
      <label><input type="radio" name="mail_protocol" value="pop3" checked="checked" /> POP3</label>
      &#160;&#160;
      <label><input type="radio" name="mail_protocol" value="imap" /> IMAP</label>
    </td>
  </tr>
  <tr>
    <td width="40%">Host</td>
    <td width="60%"><input type="text" name="mail_host" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Port<br/><small><font color="gray">Leave blank to use the protocol's default port</font></small></td>
    <td width="60%"><input type="text" name="mail_port" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Username</td>
    <td width="60%"><input type="text" name="mail_username" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Password<br/><small><font color="gray">Stored RSA-encrypted using the SFRM keystore, never shown again after saving</font></small></td>
    <td width="60%"><input type="password" name="mail_password" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Folder<br/><small><font color="gray">IMAP folder to poll (ignored for POP3, which only has one)</font></small></td>
    <td width="60%"><input type="text" name="mail_folder" size="30" value="INBOX" /></td>
  </tr>
  <tr>
    <td width="40%">Use SSL/TLS<br/><small><font color="gray">Connects via pop3s/imaps instead of plain pop3/imap</font></small></td>
    <td width="60%"><input type="checkbox" name="mail_use_ssl" checked="checked" /></td>
  </tr>
  <tr>
    <td width="40%">Polling Interval (ms)<br/><small><font color="gray">Leave blank to use the default (30000ms)</font></small></td>
    <td width="60%"><input type="text" name="mail_polling_interval" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Max Messages Per Poll<br/><small><font color="gray">Leave blank to use the default</font></small></td>
    <td width="60%"><input type="text" name="mail_max_messages_per_poll" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="mail_description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%">
      <br/>
      <span style="font-size:12px; color:var(--text-soft);">
        Messages with a ".sfrm" attachment are picked up and funneled into the outgoing repository; the message is then deleted from the mailbox.
      </span>
      <br/><br/>
      <input type="Submit" value="Add Port" /><br/>
    </td>
  </tr>
</table>
</form>
</div>

<div id="portTypeFtp" style="display:none;">
<form name="addFtpChannelForm" method="post" action="file_polling">
<input type="hidden" name="action" value="add_ftp_channel" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <td width="40%">Channel ID</td>
    <td width="60%"><input type="text" name="ftp_channel_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name</td>
    <td width="60%"><input type="text" name="ftp_name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Host</td>
    <td width="60%"><input type="text" name="ftp_host" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Port</td>
    <td width="60%"><input type="text" name="ftp_port" size="10" value="21" /></td>
  </tr>
  <tr>
    <td width="40%">Username</td>
    <td width="60%"><input type="text" name="ftp_username" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Password<br/><small><font color="gray">Stored RSA-encrypted using the SFRM keystore, never shown again after saving</font></small></td>
    <td width="60%"><input type="password" name="ftp_password" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Remote Path<br/><small><font color="gray">Directory on the FTP server to poll for ready .sfrm files</font></small></td>
    <td width="60%"><input type="text" name="ftp_remote_path" size="40" value="/" /></td>
  </tr>
  <tr>
    <td width="40%">Passive Mode<br/><small><font color="gray">Leave checked unless the server needs active mode (rare, usually behind strict firewalls)</font></small></td>
    <td width="60%"><input type="checkbox" name="ftp_passive_mode" checked="checked" /></td>
  </tr>
  <tr>
    <td width="40%">Use FTPS (TLS)<br/><small><font color="gray">Encrypts the connection. The server certificate is pinned on first connect (Trust On First Use) -- if it ever changes afterwards, the connection is refused.</font></small></td>
    <td width="60%"><input type="checkbox" name="ftp_use_tls" /></td>
  </tr>
  <tr>
    <td width="40%">Polling Interval (ms)<br/><small><font color="gray">Leave blank to use the default (30000ms)</font></small></td>
    <td width="60%"><input type="text" name="ftp_polling_interval" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Max Files Per Poll<br/><small><font color="gray">Leave blank to use the default</font></small></td>
    <td width="60%"><input type="text" name="ftp_max_files_per_poll" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="ftp_description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Add Port" /><br/></td>
  </tr>
</table>
</form>
</div>

<div id="portTypeSftp" style="display:none;">
<form name="addSftpChannelForm" method="post" action="file_polling">
<input type="hidden" name="action" value="add_sftp_channel" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <td width="40%">Channel ID</td>
    <td width="60%"><input type="text" name="sftp_channel_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name</td>
    <td width="60%"><input type="text" name="sftp_name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Host</td>
    <td width="60%"><input type="text" name="sftp_host" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Port</td>
    <td width="60%"><input type="text" name="sftp_port" size="10" value="22" /></td>
  </tr>
  <tr>
    <td width="40%">Username</td>
    <td width="60%"><input type="text" name="sftp_username" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Password<br/><small><font color="gray">Stored RSA-encrypted using the SFRM keystore, never shown again after saving</font></small></td>
    <td width="60%"><input type="password" name="sftp_password" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Remote Path<br/><small><font color="gray">Directory on the SFTP server to poll for ready .sfrm files</font></small></td>
    <td width="60%"><input type="text" name="sftp_remote_path" size="40" value="/" /></td>
  </tr>
  <tr>
    <td width="40%">Host Key<br/><small><font color="gray">Pinned automatically on first successful connect (Trust On First Use) -- if it ever changes afterwards, the connection is refused.</font></small></td>
    <td width="60%"><span class="hint">Nothing to enter here</span></td>
  </tr>
  <tr>
    <td width="40%">Polling Interval (ms)<br/><small><font color="gray">Leave blank to use the default (30000ms)</font></small></td>
    <td width="60%"><input type="text" name="sftp_polling_interval" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Max Files Per Poll<br/><small><font color="gray">Leave blank to use the default</font></small></td>
    <td width="60%"><input type="text" name="sftp_max_files_per_poll" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="sftp_description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Add Port" /><br/></td>
  </tr>
</table>
</form>
</div>

<div id="portTypeHttp" style="display:none;">
  <div style="background:var(--bg); border:1px solid var(--border); border-radius:var(--radius); padding:16px 18px; margin-bottom:12px;">
    <span style="font-size:13px; color:var(--text-soft);">
      Every protocol module (ebMS, AS2, SFRM) already listens on the shared admin/web port under
      <code>/corvus/httpd/...</code> -- adding a port here opens a separate, dedicated listener on its own
      address/port that simply forwards whatever it receives to one of those same internal services, so
      that traffic doesn't have to share the web management port.
    </span>
  </div>
<form name="addHttpChannelForm" method="post" action="file_polling">
<input type="hidden" name="action" value="add_http_channel" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <td width="40%">Channel ID</td>
    <td width="60%"><input type="text" name="http_channel_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name</td>
    <td width="60%"><input type="text" name="http_name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Bind Address<br/><small><font color="gray">Leave as 0.0.0.0 to listen on every network interface</font></small></td>
    <td width="60%"><input type="text" name="http_bind_address" size="30" value="0.0.0.0" /></td>
  </tr>
  <tr>
    <td width="40%">Port</td>
    <td width="60%"><input type="text" name="http_port" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Use HTTPS (TLS)<br/><small><font color="gray">Uses the SFRM plugin's own configured certificate</font></small></td>
    <td width="60%"><input type="checkbox" name="http_use_tls" /></td>
  </tr>
  <tr>
    <td width="40%">Target Service<br/><small><font color="gray">Which internal service handles requests received on this port</font></small></td>
    <td width="60%">
      <select name="http_target_service">
        <option value="ebms">ebMS</option>
        <option value="as2">AS2</option>
        <option value="sfrm">SFRM</option>
      </select>
    </td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="http_description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Add Port" /><br/></td>
  </tr>
</table>
</form>
</div>

<script>
function selectPortType(type) {
	document.getElementById('portTypeFilePolling').style.display = (type == 'file_polling') ? '' : 'none';
	document.getElementById('portTypeMail').style.display = (type == 'mail') ? '' : 'none';
	document.getElementById('portTypeFtp').style.display = (type == 'ftp') ? '' : 'none';
	document.getElementById('portTypeSftp').style.display = (type == 'sftp') ? '' : 'none';
	document.getElementById('portTypeHttp').style.display = (type == 'http') ? '' : 'none';
}
</script>

</xsl:template>
</xsl:stylesheet>
