<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/keys">

<div class="stat-grid">
  <div class="stat-card">
    <h3>API Keys</h3>
    <div class="stat-row"><span class="label">Total Keys</span><span class="value big"><xsl:value-of select="count(key)" /></span></div>
    <div class="stat-row"><span class="label">Active</span><span class="value big" style="color:#15803d;"><xsl:value-of select="count(key[enabled='true'])" /></span></div>
    <div class="stat-row"><span class="label">Revoked</span><span class="value" style="color:var(--text-soft);"><xsl:value-of select="count(key[enabled!='true'])" /></span></div>
  </div>
</div>

<form method="post" action="keys">
  <input type="hidden" name="action" value="add" />
  <table border="0" cellpadding="2" cellspacing="2" width="100%">
    <tr>
      <td width="30%">Client Name</td>
      <td width="70%"><input type="text" name="client_name" size="40" /></td>
    </tr>
    <tr>
      <td></td>
      <td><input type="submit" value="Generate Key" /></td>
    </tr>
  </table>
</form>

<br/>

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Client Name</th>
    <th>API Key</th>
    <th>Status</th>
    <th>Created</th>
    <th></th>
  </tr>
  <xsl:for-each select="key">
  <tr>
    <td><xsl:value-of select="./client_name" /></td>
    <td><code><xsl:value-of select="./api_key" /></code></td>
    <td>
      <xsl:choose>
        <xsl:when test="./enabled='true'"><span class="badge badge-success">Enabled</span></xsl:when>
        <xsl:otherwise><span class="badge badge-neutral">Revoked</span></xsl:otherwise>
      </xsl:choose>
    </td>
    <td><xsl:value-of select="./created" /></td>
    <td>
      <xsl:if test="./enabled='true'">
        <form method="post" action="keys" style="margin:0; display:inline;">
          <input type="hidden" name="action" value="revoke" />
          <input type="hidden" name="api_key">
            <xsl:attribute name="value"><xsl:value-of select="./api_key" /></xsl:attribute>
          </input>
          <input type="submit" value="Revoke" onclick="return confirm('Revoke this API key?');" />
        </form>
      </xsl:if>
      <form method="post" action="keys" style="margin:0; display:inline;">
        <input type="hidden" name="action" value="delete" />
        <input type="hidden" name="api_key">
          <xsl:attribute name="value"><xsl:value-of select="./api_key" /></xsl:attribute>
        </input>
        <input type="submit" value="Delete" onclick="return confirm('Permanently delete this API key? This cannot be undone.');" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>

</xsl:template>
</xsl:stylesheet>
