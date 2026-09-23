<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/home">

<div class="stat-grid">

  <div class="stat-card">
    <h3>System</h3>
    <div class="stat-row"><span class="label">Name</span><span class="value big"><xsl:value-of select="./system/name" /></span></div>
    <div class="stat-row"><span class="label">Version</span><span class="value"><xsl:value-of select="./system/version" /></span></div>
    <div class="stat-row"><span class="label">Startup Time</span><span class="value"><xsl:value-of select="./system/startup-time" /></span></div>
    <div class="stat-row"><span class="label">Current Time</span><span class="value"><xsl:value-of select="./system/current-time" /></span></div>
    <div class="stat-row"><span class="label">Up Time</span><span class="value"><xsl:value-of select="./system/up-time" /></span></div>
    <div class="stat-row"><span class="label">Processors</span><span class="value"><xsl:value-of select="./system/processors" /></span></div>
  </div>

  <div class="stat-card">
    <h3>Memory</h3>
    <div class="stat-row"><span class="label">Available</span><span class="value"><xsl:value-of select="./memory/max" /></span></div>
    <div class="stat-row"><span class="label">Allocated</span><span class="value"><xsl:value-of select="./memory/total" /></span></div>
    <div class="stat-row"><span class="label">Used</span><span class="value"><xsl:value-of select="./memory/used" /></span></div>
    <div class="stat-row"><span class="label">Free</span><span class="value"><xsl:value-of select="./memory/free" /></span></div>
    <div class="stat-row"><span class="label">Usage</span><span class="value big"><xsl:value-of select="./memory/usage" /></span></div>
    <div class="progress">
      <div class="progress-fill">
        <xsl:attribute name="style">width: <xsl:value-of select="substring-before(./memory/usage,'%')"/>%</xsl:attribute>
      </div>
    </div>
  </div>

</div>

<form>
  <div class="actions">
    <input type="button" value="Garbage Collection">
      <xsl:attribute name="onclick">if (confirm('Are you sure to run garbage collection?')) {document.location='?action=gc';}</xsl:attribute>
    </input>
    <input type="button" value="Finalization">
      <xsl:attribute name="onclick">if (confirm('Are you sure to run finalization?')) {document.location='?action=final';}</xsl:attribute>
    </input>
    <input type="button" value="Refresh" onclick="document.location='?action=refresh'"/>
  </div>
</form>

</xsl:template>
</xsl:stylesheet>
