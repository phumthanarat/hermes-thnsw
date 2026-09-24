<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/security">
	<br/>
	<p style="color:var(--text-soft);">
	  Apply to every console user. A new password rule applies at the next password change;
	  lock-out and session settings apply straight away.
	</p>
	<form method="post" action="security">
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <xsl:for-each select="setting">
	    <tr>
	      <td width="55%"><xsl:value-of select="label" /></td>
	      <td>
	        <xsl:choose>
	          <xsl:when test="type='boolean'">
	            <input type="checkbox" value="true">
	              <xsl:attribute name="name"><xsl:value-of select="name" /></xsl:attribute>
	              <xsl:if test="value='true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
	            </input>
	          </xsl:when>
	          <xsl:otherwise>
	            <input type="number" size="6" required="required">
	              <xsl:attribute name="name"><xsl:value-of select="name" /></xsl:attribute>
	              <xsl:attribute name="value"><xsl:value-of select="value" /></xsl:attribute>
	              <xsl:attribute name="min"><xsl:value-of select="min" /></xsl:attribute>
	              <xsl:attribute name="max"><xsl:value-of select="max" /></xsl:attribute>
	            </input>
	          </xsl:otherwise>
	        </xsl:choose>
	      </td>
	    </tr>
	  </xsl:for-each>
	  <tr><td></td><td><br/><input type="submit" value="Save" /></td></tr>
	</table>
	</form>
</xsl:template>
</xsl:stylesheet>
