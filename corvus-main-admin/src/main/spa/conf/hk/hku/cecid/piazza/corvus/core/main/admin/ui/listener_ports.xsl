<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/listener_ports">

<div style="background:var(--bg); border:1px solid var(--border); border-radius:var(--radius); padding:16px 18px; margin-bottom:12px;">
  <span style="font-size:13px; color:var(--text-soft);">
    ebMS, AS2 and SFRM already accept inbound traffic on the shared admin/web
    port under <code>/corvus/httpd/...</code> -- that is always on and needs
    no configuration here. A listener port below just gives one partner a
    second, dedicated address:port (e.g. https://203.0.113.10:5443) to reach
    that same handling on, separate from every other partner's own port.
  </span>
</div>

<div class="stat-grid">
  <div class="stat-card">
    <h3>Listener Ports</h3>
    <div class="stat-row"><span class="label">Total Listener Ports</span><span class="value big"><xsl:value-of select="count(ports/port)" /></span></div>
  </div>
</div>

<xsl:if test="count(ports/port) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Port ID</th>
    <th>Name</th>
    <th>Listens On</th>
    <th>Target Service</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="ports/port">
  <tr>
    <td><code><xsl:value-of select="./port_id" /></code></td>
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
      <form method="post" action="listenerports" style="display:inline;">
        <input type="hidden" name="action" value="toggle_listener_port" />
        <input type="hidden" name="port_id"><xsl:attribute name="value"><xsl:value-of select="./port_id" /></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./is_disabled = 'true'">
            <input type="submit" value="Enable" />
          </xsl:when>
          <xsl:otherwise>
            <input type="submit" value="Disable" />
          </xsl:otherwise>
        </xsl:choose>
      </form>
      <form method="post" action="listenerports" style="display:inline;" onSubmit="return confirm('Delete this listener port?');">
        <input type="hidden" name="action" value="delete_listener_port" />
        <input type="hidden" name="port_id"><xsl:attribute name="value"><xsl:value-of select="./port_id" /></xsl:attribute></input>
        <input type="submit" value="Delete" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<form name="addListenerPortForm" method="post" action="listenerports">
<input type="hidden" name="action" value="add_listener_port" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Add Listener Port</th>
  </tr>
  <tr>
    <td width="40%">Port ID</td>
    <td width="60%"><input type="text" name="port_id" size="30" /></td>
  </tr>
  <tr>
    <td width="40%">Name<br/><small><font color="gray">e.g. the partner this port is dedicated to</font></small></td>
    <td width="60%"><input type="text" name="name" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Bind Address<br/><small><font color="gray">Leave as 0.0.0.0 to listen on every network interface</font></small></td>
    <td width="60%"><input type="text" name="bind_address" size="30" value="0.0.0.0" /></td>
  </tr>
  <tr>
    <td width="40%">Port</td>
    <td width="60%"><input type="text" name="port" size="10" /></td>
  </tr>
  <tr>
    <td width="40%">Use HTTPS (TLS)<br/><small><font color="gray">Uses the gateway's own configured certificate</font></small></td>
    <td width="60%"><input type="checkbox" name="use_tls" /></td>
  </tr>
  <tr>
    <td width="40%">Target Service<br/><small><font color="gray">Which internal service handles requests received on this port</font></small></td>
    <td width="60%">
      <select name="target_service">
        <option value="ebms">ebMS</option>
        <option value="as2">AS2</option>
        <option value="sfrm">SFRM</option>
      </select>
    </td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Add Listener Port" /><br/></td>
  </tr>
</table>
</form>

</xsl:template>
</xsl:stylesheet>
