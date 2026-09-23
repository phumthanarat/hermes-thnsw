<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/error">
<div class="error-banner">
  <h3>Plugin activation error</h3>
  <div class="stat-row"><span class="label">Plugin</span><span class="value"><xsl:value-of select="./plugin" /></span></div>
  <div class="stat-row"><span class="label">Details</span><span class="value">See the log in <xsl:value-of select="./home_directory" /></span></div>
  <div class="stat-row"><span class="label">Time</span><span class="value"><xsl:value-of select="./time" /></span></div>
</div>
</xsl:template>
</xsl:stylesheet>
