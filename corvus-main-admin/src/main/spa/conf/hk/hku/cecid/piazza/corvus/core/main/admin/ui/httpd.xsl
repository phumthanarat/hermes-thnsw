<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/httpd">

<div class="stat-grid">
  <div class="stat-card">
    <h3>HTTP Dispatcher</h3>
    <div class="stat-row"><span class="label">Status</span>
      <span class="value">
        <xsl:choose>
          <xsl:when test="contains(./status/state, 'Halt')"><span class="badge badge-danger"><xsl:value-of select="./status/state" /></span></xsl:when>
          <xsl:otherwise><span class="badge badge-success"><xsl:value-of select="./status/state" /></span></xsl:otherwise>
        </xsl:choose>
      </span>
    </div>
    <div class="stat-row"><span class="label">Current Threads</span><span class="value big"><xsl:value-of select="./status/threads" /></span></div>
  </div>
  <div class="stat-card">
    <h3>Registered Listeners</h3>
    <div class="stat-row"><span class="label">Context Listeners</span><span class="value"><xsl:value-of select="count(context-listeners/listener)" /></span></div>
    <div class="stat-row"><span class="label">Request Filters</span><span class="value"><xsl:value-of select="count(request-filters/listener)" /></span></div>
    <div class="stat-row"><span class="label">Request Listeners</span><span class="value big"><xsl:value-of select="count(request-listeners/listener)" /></span></div>
  </div>
</div>

<br/>
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="5">Web Console Ports (Tomcat) &#8212; separate from data-transfer ports under the "Port" module</th>
  </tr>
  <tr>
    <th width="15%">Port</th>
    <th width="15%">Number</th>
    <th width="15%">Status</th>
    <th width="30%">Change Port</th>
    <th width="25%"></th>
  </tr>
  <xsl:for-each select="connectors/connector">
  <tr>
    <td><xsl:value-of select="./name" /></td>
    <td><xsl:value-of select="./port" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./enabled='true'"><span class="badge badge-success">ENABLED</span></xsl:when>
        <xsl:otherwise><span class="badge badge-neutral">DISABLED</span></xsl:otherwise>
      </xsl:choose>
    </td>
    <td>
      <form method="get" style="display:inline;">
        <input type="hidden" name="action" value="update_connector_port"/>
        <input type="hidden" name="connector"><xsl:attribute name="value"><xsl:value-of select="./id"/></xsl:attribute></input>
        <input type="text" name="port" size="6"><xsl:attribute name="value"><xsl:value-of select="./port"/></xsl:attribute></input>
        <input type="submit" value="Save"/>
      </form>
    </td>
    <td>
      <form method="get" style="display:inline;" onSubmit="return confirm('Change takes effect only after the container is restarted. Continue?');">
        <input type="hidden" name="action" value="toggle_connector"/>
        <input type="hidden" name="connector"><xsl:attribute name="value"><xsl:value-of select="./id"/></xsl:attribute></input>
        <xsl:choose>
          <xsl:when test="./enabled='true'"><input type="submit" value="Disable"/></xsl:when>
          <xsl:otherwise><input type="submit" value="Enable"/></xsl:otherwise>
        </xsl:choose>
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<p>
  <xsl:if test="restart-required='true'">
    <span class="badge badge-danger">RESTART REQUIRED</span><xsl:text> </xsl:text>
  </xsl:if>
  <small>Port changes are saved to server.xml immediately but only take effect after the container is restarted (<code>docker compose restart app</code>). A port cannot be disabled while it is the only one enabled, to avoid locking the console out entirely.</small>
</p>

<xsl:if test="ping_test/result">
<div style="background:var(--bg); border:1px solid var(--border); border-radius:var(--radius); padding:12px 16px; margin-bottom:12px;">
  <xsl:choose>
    <xsl:when test="ping_test/result = 'success'">
      <span class="badge badge-success">pong</span>
    </xsl:when>
    <xsl:otherwise>
      <span class="badge badge-danger">Failed</span>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text> </xsl:text>
  <span style="font-size:13px;"><xsl:value-of select="ping_test/target" /> -- <xsl:value-of select="ping_test/message" /></span>
</div>
</xsl:if>

<form name="pingTestForm" method="post" action="httpd">
<input type="hidden" name="action" value="ping_test" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Ping Test</th>
  </tr>
  <tr>
    <td width="30%">Target<br/><small><font color="gray">Defaults to this gateway's own WSPingService; point it at another Hermes instance's /corvus/httpd/wsping to check server-to-server reachability</font></small></td>
    <td width="70%">
      <input type="text" name="target_url" size="60">
        <xsl:attribute name="value"><xsl:value-of select="ping_test/target" /><xsl:if test="not(ping_test/target)"><xsl:value-of select="ping_test/default_target" /></xsl:if></xsl:attribute>
      </input>
    </td>
  </tr>
  <tr>
    <td width="30%"></td>
    <td width="70%"><br/><input type="Submit" value="Ping" /><br/></td>
  </tr>
</table>
</form>

<br/>

<form>
<br/>
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2">Request Listener Details</th>
  </tr>
  <tr>
    <th width="30%">Context</th>
    <th width="70%">Listener</th>
  </tr>

  <xsl:for-each select="request-listeners/listener">
  <tr>
    <td><xsl:value-of select="./context" /></td>
    <td><xsl:value-of select="./listener" /></td>
  </tr>
  </xsl:for-each>

</table>

<p align="right">
<xsl:choose>
   <xsl:when test="./status/action='resume'">
      <input type="button" value="Resume">
      	<xsl:attribute name="onclick">if (confirm('Are you sure to resume the dispatcher?')) {document.location='./httpd?action=resume';}</xsl:attribute>
      </input>
   </xsl:when>
   <xsl:when test="./status/action='halt'">
      <input type="button" value="Halt">
      	<xsl:attribute name="onclick">if (confirm('Are you sure to halt the dispatcher?')) {document.location='./httpd?action=halt';}</xsl:attribute>
      </input>
   </xsl:when>
   <xsl:otherwise/>
</xsl:choose>
<xsl:text>  </xsl:text>
<input type="button" value="Refresh" onclick="document.location='./httpd'"/>
</p>

</form>
 
</xsl:template>
</xsl:stylesheet>