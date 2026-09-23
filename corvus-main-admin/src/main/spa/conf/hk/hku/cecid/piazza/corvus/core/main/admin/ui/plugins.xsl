<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/registry">

<div class="stat-grid">
  <div class="stat-card">
    <h3>Plugin Registry</h3>
    <div class="stat-row"><span class="label">Location</span><span class="value" style="font-size:12px;"><xsl:value-of select="./location" /></span></div>
    <div class="stat-row"><span class="label">Activation</span><span class="value"><xsl:value-of select="./activation" /></span></div>
    <div class="stat-row"><span class="label">Registered Plugins</span><span class="value big"><xsl:value-of select="count(plugins/plugin)" /></span></div>
  </div>
</div>

<form>
<br/>
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2">Registered Plugins</th>
  </tr>
  <xsl:for-each select="plugins/plugin">
  <tr>
    <th colspan="2"/>
  </tr>
  <tr>
    <td><b>ID</b></td>
    <td><b><xsl:value-of select="./id" /></b></td>
  </tr>
  <tr>
    <td>Name</td>
    <td><xsl:value-of select="./name" /></td>
  </tr>
    <tr>
    <td>Version</td>
    <td><xsl:value-of select="./version" /></td>
  </tr>
  <tr>
    <td>Extension Points</td>
    <td><xsl:value-of select="./points" /></td>
  </tr>
  <tr>
    <td>Extensions</td>
    <td><xsl:value-of select="./extensions" /></td>
  </tr>
  </xsl:for-each>

</table>
</form>
 
</xsl:template>
</xsl:stylesheet>