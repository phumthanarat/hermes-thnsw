<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/webservice">

<div class="stat-grid">
  <div class="stat-card">
    <h3>Plugin</h3>
    <div class="stat-row"><span class="label">Name</span><span class="value big"><xsl:value-of select="./plugin/name" /></span></div>
    <div class="stat-row"><span class="label">Status</span><span class="value"><xsl:value-of select="./plugin/status" /></span></div>
  </div>
  <div class="stat-card">
    <h3>Authentication</h3>
    <div class="stat-row"><span class="label">Method</span><span class="value"><xsl:value-of select="./auth/method" /></span></div>
    <div class="stat-row"><span class="label">Header</span><span class="value"><code><xsl:value-of select="./auth/header" /></code></span></div>
    <div class="stat-row"><span class="label">Active Keys</span><span class="value big"><xsl:value-of select="./auth/enabled_keys" /> / <xsl:value-of select="./auth/total_keys" /></span></div>
  </div>
</div>

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Path</th>
    <th>Full URL</th>
    <th>Listener</th>
  </tr>
  <xsl:for-each select="endpoints/endpoint">
  <tr>
    <td><xsl:value-of select="./path" /></td>
    <td><xsl:value-of select="./url" /></td>
    <td><xsl:value-of select="./listener" /></td>
  </tr>
  </xsl:for-each>
</table>

</xsl:template>
</xsl:stylesheet>
